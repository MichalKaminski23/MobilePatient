-- ============================================================================
-- POLITYKI RLS DLA ROLI ADMINISTRATORA (ADMIN) ORAZ AUDYTU (T11)
-- ============================================================================

-- 0. Funkcja pomocnicza weryfikująca rolę administratora (SECURITY DEFINER)
-- WAŻNE: Musi posiadać SECURITY DEFINER, aby zapobiec nieskończonej rekurencji (infinite recursion)
-- podczas ewaluacji polityk RLS na tabeli public.profiles!
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS boolean AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid() AND role = 'admin'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 1. Tabela clinics: Administrator może zarządzać siecią placówek (INSERT, UPDATE, DELETE)
DROP POLICY IF EXISTS "Admin clinics insert" ON public.clinics;
CREATE POLICY "Admin clinics insert" ON public.clinics
FOR INSERT WITH CHECK (
    public.is_admin()
);

DROP POLICY IF EXISTS "Admin clinics update" ON public.clinics;
CREATE POLICY "Admin clinics update" ON public.clinics
FOR UPDATE USING (
    public.is_admin()
) WITH CHECK (
    public.is_admin()
);

DROP POLICY IF EXISTS "Admin clinics delete" ON public.clinics;
CREATE POLICY "Admin clinics delete" ON public.clinics
FOR DELETE USING (
    public.is_admin()
);

-- 2. Tabela audit_logs: Rejestr audytu i niezaprzeczalności (T11 / MASVS-CODE)
-- a) Administrator ma pełny wgląd do wszystkich logów audytowych w systemie
DROP POLICY IF EXISTS "Admin audit_logs select" ON public.audit_logs;
CREATE POLICY "Admin audit_logs select" ON public.audit_logs
FOR SELECT USING (
    public.is_admin()
);

-- b) Zalogowani użytkownicy (lub system) mogą rejestrować operacje w dzienniku audytu
DROP POLICY IF EXISTS "Zalogowani moga tworzyc audit_logs" ON public.audit_logs;
CREATE POLICY "Zalogowani moga tworzyc audit_logs" ON public.audit_logs
FOR INSERT WITH CHECK (
    auth.uid() IS NOT NULL
);

-- 3. Tabela profiles: Administrator może przeglądać profile, rejestrować lekarzy i nadawać role
-- Użycie public.is_admin() eliminuje błąd 42P17 (infinite recursion detected in policy for relation "profiles")
DROP POLICY IF EXISTS "Admin profiles select" ON public.profiles;
CREATE POLICY "Admin profiles select" ON public.profiles
FOR SELECT USING (
    public.is_admin()
);

DROP POLICY IF EXISTS "Admin profiles insert" ON public.profiles;
CREATE POLICY "Admin profiles insert" ON public.profiles
FOR INSERT WITH CHECK (
    public.is_admin()
);

DROP POLICY IF EXISTS "Admin profiles update role" ON public.profiles;
CREATE POLICY "Admin profiles update role" ON public.profiles
FOR UPDATE USING (
    public.is_admin()
) WITH CHECK (
    public.is_admin()
);
