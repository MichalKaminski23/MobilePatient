-- Włączenie rozszerzenia do generowania UUID (domyślnie aktywne w Supabase)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- 1. PROFILE UŻYTKOWNIKÓW (zintegrowane z auth.users)
-- ============================================================================
CREATE TABLE public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'patient' CHECK (role IN ('patient', 'doctor', 'admin')),
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    pesel TEXT UNIQUE,
    phone TEXT,
    address TEXT,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- ============================================================================
-- 2. KATALOG PRZYCHODNI (Dane publiczne dla mapy)
-- ============================================================================
CREATE TABLE public.clinics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    address TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    phone TEXT,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- ============================================================================
-- 3. ASOCJACJA LEKARZ - PACJENT (Kontrola dostępu i relacji)
-- ============================================================================
CREATE TABLE public.doctor_patients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    CONSTRAINT uq_doctor_patient UNIQUE (doctor_id, patient_id)
);

-- ============================================================================
-- 4. WIZYTY LEKARSKIE
-- ============================================================================
CREATE TABLE public.appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    clinic_id UUID NOT NULL REFERENCES public.clinics(id) ON DELETE RESTRICT,
    scheduled_at TIMESTAMPTZ NOT NULL,
    status TEXT NOT NULL DEFAULT 'scheduled' CHECK (status IN ('scheduled', 'completed', 'cancelled')),
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- ============================================================================
-- 5. HISTORIA I DOKUMENTACJA MEDYCZNA (Dane szczególnie wrażliwe)
-- ============================================================================
CREATE TABLE public.medical_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    appointment_id UUID REFERENCES public.appointments(id) ON DELETE SET NULL,
    record_type TEXT NOT NULL CHECK (record_type IN ('visit', 'test_result', 'referral')),
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- ============================================================================
-- 6. E-RECEPTY
-- ============================================================================
CREATE TABLE public.prescriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    medical_record_id UUID REFERENCES public.medical_records(id) ON DELETE SET NULL,
    code CHAR(4) NOT NULL,
    medications TEXT NOT NULL,
    issued_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    valid_until DATE NOT NULL,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'fulfilled', 'expired', 'cancelled'))
);

-- ============================================================================
-- 7. METADANE PLIKÓW I SKANÓW (Powiązanie z Supabase Storage)
-- ============================================================================
CREATE TABLE public.medical_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    uploaded_by UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    medical_record_id UUID REFERENCES public.medical_records(id) ON DELETE SET NULL,
    storage_path TEXT NOT NULL,
    file_name TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- ============================================================================
-- 8. DZIENNIK AUDYTOWY (Append-Only — ochrona przed niezaprzeczalnością)
-- ============================================================================
CREATE TABLE public.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id UUID,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- ============================================================================
-- 9. INDEKSY WYDAJNOŚCIOWE
-- ============================================================================
CREATE INDEX idx_appointments_patient ON public.appointments(patient_id, scheduled_at);
CREATE INDEX idx_appointments_doctor ON public.appointments(doctor_id, scheduled_at);
CREATE INDEX idx_medical_records_patient ON public.medical_records(patient_id);
CREATE INDEX idx_prescriptions_patient ON public.prescriptions(patient_id);
CREATE INDEX idx_medical_files_record ON public.medical_files(medical_record_id);
CREATE INDEX idx_audit_logs_actor ON public.audit_logs(actor_id);

-- ============================================================================
-- 10. WYMOGI BEZPIECZEŃSTWA: AKTYWACJA ROW-LEVEL SECURITY (RLS)
-- ============================================================================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.clinics ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.doctor_patients ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.prescriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_files ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- Podstawowa polityka dla przychodni (publiczny odczyt dla modułu mapy)
CREATE POLICY "Publiczny odczyt placówek" ON public.clinics
FOR SELECT USING (true);

-- Podstawowa polityka dla profili (użytkownik widzi i edytuje swój profil)
CREATE POLICY "Odczyt własnego profilu" ON public.profiles
FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Aktualizacja własnego profilu" ON public.profiles
FOR UPDATE USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);

-- ============================================================================
-- 11. TRIGGER: AUTOMATYCZNE TWORZENIE PROFILU PO REJESTRACJI W AUTH
-- ============================================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
    INSERT INTO public.profiles (id, first_name, last_name, role)
    VALUES (
        new.id,
        COALESCE(new.raw_user_meta_data->>'first_name', 'Imię'),
        COALESCE(new.raw_user_meta_data->>'last_name', 'Nazwisko'),
        COALESCE(new.raw_user_meta_data->>'role', 'patient')
    );
    RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ============================================================================
-- 12. PRYWATNY BUCKET NA SKANY MEDYCZNE W STORAGE
-- ============================================================================
INSERT INTO storage.buckets (id, name, public)
VALUES ('skany-medyczne', 'skany-medyczne', false)
ON CONFLICT (id) DO NOTHING;