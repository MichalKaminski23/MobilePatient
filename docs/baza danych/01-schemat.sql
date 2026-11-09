-- Rozszerzenia wymagane przez schemat bazy.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabele aplikacyjne. Schemat auth.users jest zarządzany przez Supabase Auth.

CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'patient' CHECK (role IN ('patient', 'doctor', 'admin')),
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    pesel TEXT UNIQUE,
    phone TEXT,
    address TEXT,
    must_change_password BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.clinics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    address TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    phone TEXT,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.doctor_patients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    CONSTRAINT uq_doctor_patient UNIQUE (doctor_id, patient_id)
);

CREATE TABLE IF NOT EXISTS public.appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    clinic_id UUID NOT NULL REFERENCES public.clinics(id) ON DELETE RESTRICT,
    scheduled_at TIMESTAMPTZ NOT NULL,
    status TEXT NOT NULL DEFAULT 'scheduled' CHECK (status IN ('scheduled', 'completed', 'cancelled')),
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.medical_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    appointment_id UUID REFERENCES public.appointments(id) ON DELETE SET NULL,
    record_type TEXT NOT NULL CHECK (record_type IN ('visit', 'test_result', 'referral')),
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.prescriptions (
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

CREATE TABLE IF NOT EXISTS public.medical_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    uploaded_by UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    medical_record_id UUID REFERENCES public.medical_records(id) ON DELETE SET NULL,
    storage_path TEXT NOT NULL,
    file_name TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id UUID,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Indeksy wspierające najczęstsze zapytania aplikacji.

CREATE INDEX IF NOT EXISTS idx_appointments_patient
    ON public.appointments(patient_id, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor
    ON public.appointments(doctor_id, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_medical_records_patient
    ON public.medical_records(patient_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_patient
    ON public.prescriptions(patient_id);
CREATE INDEX IF NOT EXISTS idx_medical_files_record
    ON public.medical_files(medical_record_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor
    ON public.audit_logs(actor_id);