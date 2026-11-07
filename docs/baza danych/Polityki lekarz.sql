-- ============================================================================
-- POLITYKI RLS DLA ROLI LEKARZA (DOCTOR)
-- ============================================================================

-- 1. Tabela doctor_patients: Lekarz widzi powiązania ze swoimi pacjentami
DROP POLICY IF EXISTS "Lekarz widzi doctor_patients" ON public.doctor_patients;
CREATE POLICY "Lekarz widzi doctor_patients" ON public.doctor_patients
FOR SELECT USING (auth.uid() = doctor_id);

-- 2. Tabela profiles: Lekarz widzi swój profil oraz profile pacjentów w systemie
DROP POLICY IF EXISTS "Lekarz widzi profile pacjentów" ON public.profiles;
CREATE POLICY "Lekarz widzi profile pacjentów" ON public.profiles
FOR SELECT USING (
    auth.uid() = id 
    OR role = 'patient'
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

-- 5. Tabela appointments: Lekarz widzi i aktualizuje wizyty
DROP POLICY IF EXISTS "Lekarz appointments select" ON public.appointments;
CREATE POLICY "Lekarz appointments select" ON public.appointments
FOR SELECT USING (doctor_id = auth.uid() OR patient_id = auth.uid());

DROP POLICY IF EXISTS "Lekarz appointments update" ON public.appointments;
CREATE POLICY "Lekarz appointments update" ON public.appointments
FOR UPDATE USING (doctor_id = auth.uid());