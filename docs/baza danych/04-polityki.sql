-- Włączenie Row-Level Security na wszystkich tabelach aplikacyjnych.

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.clinics ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.doctor_patients ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.prescriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_files ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- Podstawowy dostęp: publiczny odczyt placówek oraz własny profil użytkownika.

DROP POLICY IF EXISTS "Publiczny odczyt placówek" ON public.clinics;
CREATE POLICY "Publiczny odczyt placówek" ON public.clinics
FOR SELECT USING (true);

DROP POLICY IF EXISTS "Odczyt własnego profilu" ON public.profiles;
CREATE POLICY "Odczyt własnego profilu" ON public.profiles
FOR SELECT TO authenticated USING (auth.uid() = id);

DROP POLICY IF EXISTS "Aktualizacja własnego profilu" ON public.profiles;
CREATE POLICY "Aktualizacja własnego profilu" ON public.profiles
FOR UPDATE USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);

-- Polityki RLS dla lekarza i pacjenta.

DROP POLICY IF EXISTS "Lekarz widzi doctor_patients" ON public.doctor_patients;
CREATE POLICY "Lekarz widzi doctor_patients" ON public.doctor_patients
FOR SELECT USING (auth.uid() = doctor_id);

DROP POLICY IF EXISTS "Lekarz insert doctor_patients" ON public.doctor_patients;
CREATE POLICY "Lekarz insert doctor_patients" ON public.doctor_patients
FOR INSERT WITH CHECK (doctor_id = auth.uid());

DROP POLICY IF EXISTS "Lekarz delete doctor_patients" ON public.doctor_patients;
CREATE POLICY "Lekarz delete doctor_patients" ON public.doctor_patients
FOR DELETE USING (doctor_id = auth.uid());

DROP POLICY IF EXISTS "Lekarz widzi profile pacjentów" ON public.profiles;
CREATE POLICY "Lekarz widzi profile pacjentów" ON public.profiles
FOR SELECT USING (
    auth.uid() = id
    OR role = 'patient'
    OR role = 'doctor'
    OR EXISTS (
        SELECT 1 FROM public.doctor_patients
        WHERE doctor_id = auth.uid() AND patient_id = profiles.id
    )
);

DROP POLICY IF EXISTS "Lekarz medical_records select" ON public.medical_records;
CREATE POLICY "Lekarz medical_records select" ON public.medical_records
FOR SELECT USING (doctor_id = auth.uid() OR patient_id = auth.uid());

DROP POLICY IF EXISTS "Lekarz medical_records insert" ON public.medical_records;
CREATE POLICY "Lekarz medical_records insert" ON public.medical_records
FOR INSERT WITH CHECK (doctor_id = auth.uid());

DROP POLICY IF EXISTS "Lekarz prescriptions select" ON public.prescriptions;
CREATE POLICY "Lekarz prescriptions select" ON public.prescriptions
FOR SELECT USING (doctor_id = auth.uid() OR patient_id = auth.uid());

DROP POLICY IF EXISTS "Lekarz prescriptions insert" ON public.prescriptions;
CREATE POLICY "Lekarz prescriptions insert" ON public.prescriptions
FOR INSERT WITH CHECK (doctor_id = auth.uid());

DROP POLICY IF EXISTS "Prescriptions update (lekarz lub pacjent)" ON public.prescriptions;
CREATE POLICY "Prescriptions update (lekarz lub pacjent)" ON public.prescriptions
FOR UPDATE USING (doctor_id = auth.uid() OR patient_id = auth.uid());

DROP POLICY IF EXISTS "Lekarz appointments select" ON public.appointments;
CREATE POLICY "Lekarz appointments select" ON public.appointments
FOR SELECT USING (doctor_id = auth.uid() OR patient_id = auth.uid());

DROP POLICY IF EXISTS "Appointments insert (lekarz lub pacjent)" ON public.appointments;
CREATE POLICY "Appointments insert (lekarz lub pacjent)" ON public.appointments
FOR INSERT WITH CHECK (doctor_id = auth.uid() OR patient_id = auth.uid());

DROP POLICY IF EXISTS "Appointments update (lekarz lub pacjent)" ON public.appointments;
CREATE POLICY "Appointments update (lekarz lub pacjent)" ON public.appointments
FOR UPDATE USING (doctor_id = auth.uid() OR patient_id = auth.uid());

DROP POLICY IF EXISTS "Dostęp do medical_files" ON public.medical_files;
CREATE POLICY "Dostęp do medical_files" ON public.medical_files
FOR ALL USING (patient_id = auth.uid() OR uploaded_by = auth.uid())
WITH CHECK (patient_id = auth.uid() OR uploaded_by = auth.uid());

DROP POLICY IF EXISTS "Dostęp do bucketu medical-scans" ON storage.objects;
CREATE POLICY "Dostęp do bucketu medical-scans" ON storage.objects
FOR ALL USING (bucket_id = 'medical-scans')
WITH CHECK (bucket_id = 'medical-scans');

-- Polityki RLS dla administratora oraz zapisu dziennika audytowego.

CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS boolean AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid() AND role = 'admin'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP POLICY IF EXISTS "Admin clinics insert" ON public.clinics;
CREATE POLICY "Admin clinics insert" ON public.clinics
FOR INSERT WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "Admin clinics update" ON public.clinics;
CREATE POLICY "Admin clinics update" ON public.clinics
FOR UPDATE USING (public.is_admin()) WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "Admin clinics delete" ON public.clinics;
CREATE POLICY "Admin clinics delete" ON public.clinics
FOR DELETE USING (public.is_admin());

DROP POLICY IF EXISTS "Admin audit_logs select" ON public.audit_logs;
CREATE POLICY "Admin audit_logs select" ON public.audit_logs
FOR SELECT USING (public.is_admin());

DROP POLICY IF EXISTS "Zalogowani moga tworzyc audit_logs" ON public.audit_logs;
CREATE POLICY "Zalogowani moga tworzyc audit_logs" ON public.audit_logs
FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS "Admin profiles select" ON public.profiles;
CREATE POLICY "Admin profiles select" ON public.profiles
FOR SELECT USING (public.is_admin());

DROP POLICY IF EXISTS "Admin profiles insert" ON public.profiles;
CREATE POLICY "Admin profiles insert" ON public.profiles
FOR INSERT WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "Admin profiles update role" ON public.profiles;
CREATE POLICY "Admin profiles update role" ON public.profiles
FOR UPDATE USING (public.is_admin()) WITH CHECK (public.is_admin());