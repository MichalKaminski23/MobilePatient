-- ============================================================================
-- POLITYKI RLS DLA ROLI LEKARZA (DOCTOR)
-- ============================================================================

-- 1. Tabela doctor_patients: Lekarz widzi powiązania ze swoimi pacjentami
DROP POLICY IF EXISTS "Lekarz widzi doctor_patients" ON public.doctor_patients;
CREATE POLICY "Lekarz widzi doctor_patients" ON public.doctor_patients
FOR SELECT USING (auth.uid() = doctor_id);

-- 2. Tabela profiles: Lekarz widzi swój profil oraz profile pacjentów, a pacjenci widzą lekarzy
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

-- 3. Tabela medical_records: Lekarz widzi i dodaje wpisy dla pacjentów
DROP POLICY IF EXISTS "Lekarz medical_records select" ON public.medical_records;
CREATE POLICY "Lekarz medical_records select" ON public.medical_records
FOR SELECT USING (doctor_id = auth.uid() OR patient_id = auth.uid());

DROP POLICY IF EXISTS "Lekarz medical_records insert" ON public.medical_records;
CREATE POLICY "Lekarz medical_records insert" ON public.medical_records
FOR INSERT WITH CHECK (doctor_id = auth.uid());

-- 4. Tabela prescriptions: Lekarz widzi i wystawia e-recepty
DROP POLICY IF EXISTS "Lekarz prescriptions select" ON public.prescriptions;
CREATE POLICY "Lekarz prescriptions select" ON public.prescriptions
FOR SELECT USING (doctor_id = auth.uid() OR patient_id = auth.uid());

DROP POLICY IF EXISTS "Lekarz prescriptions insert" ON public.prescriptions;
CREATE POLICY "Lekarz prescriptions insert" ON public.prescriptions
FOR INSERT WITH CHECK (doctor_id = auth.uid());

DROP POLICY IF EXISTS "Prescriptions update (lekarz lub pacjent)" ON public.prescriptions;
CREATE POLICY "Prescriptions update (lekarz lub pacjent)" ON public.prescriptions
FOR UPDATE USING (doctor_id = auth.uid() OR patient_id = auth.uid());

-- 5. Tabela appointments: Lekarz i pacjent widzą, tworzą i aktualizują (np. odwołanie) wizyty
DROP POLICY IF EXISTS "Lekarz appointments select" ON public.appointments;
CREATE POLICY "Lekarz appointments select" ON public.appointments
FOR SELECT USING (doctor_id = auth.uid() OR patient_id = auth.uid());

DROP POLICY IF EXISTS "Appointments insert (lekarz lub pacjent)" ON public.appointments;
CREATE POLICY "Appointments insert (lekarz lub pacjent)" ON public.appointments
FOR INSERT WITH CHECK (doctor_id = auth.uid() OR patient_id = auth.uid());

DROP POLICY IF EXISTS "Appointments update (lekarz lub pacjent)" ON public.appointments;
CREATE POLICY "Appointments update (lekarz lub pacjent)" ON public.appointments
FOR UPDATE USING (doctor_id = auth.uid() OR patient_id = auth.uid());

-- 6. Tabela medical_files i Supabase Storage (skany-medyczne)
DROP POLICY IF EXISTS "Dostęp do medical_files" ON public.medical_files;
CREATE POLICY "Dostęp do medical_files" ON public.medical_files
FOR ALL USING (patient_id = auth.uid() OR uploaded_by = auth.uid())
WITH CHECK (patient_id = auth.uid() OR uploaded_by = auth.uid());

-- 7. Bucket skany-medyczne w storage.objects
DROP POLICY IF EXISTS "Dostęp do bucketu skany-medyczne" ON storage.objects;
CREATE POLICY "Dostęp do bucketu skany-medyczne" ON storage.objects
FOR ALL USING (bucket_id = 'skany-medyczne')
WITH CHECK (bucket_id = 'skany-medyczne');