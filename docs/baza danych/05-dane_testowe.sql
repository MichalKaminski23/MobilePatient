-- Minimalne dane testowe.
-- UWAGA: skrypt usuwa dane z auth.users i tabel aplikacyjnych.

TRUNCATE TABLE
    auth.users,
    public.audit_logs,
    public.medical_files,
    public.prescriptions,
    public.medical_records,
    public.appointments,
    public.doctor_patients,
    public.clinics,
    public.profiles
CASCADE;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

DO $$
DECLARE
    v_admin_id UUID := '08aa52bc-4a65-4465-acb5-143c0a411451';
    v_doctor_id UUID := 'daf781ca-3d91-4de0-9f89-44ea123159f5';
    v_patient_id UUID := '9b71063b-4cb8-4d4b-a71a-4dbeb3ae776c';
    v_clinic_id UUID := '44444444-4444-4444-4444-444444444444';
    v_appointment_id UUID := '55555555-5555-5555-5555-555555555555';
    v_record_id UUID := '66666666-6666-6666-6666-666666666666';
BEGIN
    INSERT INTO auth.users (
        id,
        instance_id,
        aud,
        role,
        email,
        encrypted_password,
        email_confirmed_at,
        raw_app_meta_data,
        raw_user_meta_data,
        created_at,
        updated_at,
        confirmation_token,
        recovery_token,
        email_change_token_new,
        email_change
    )
    VALUES
        (v_admin_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'admin@test.pl', crypt('test123!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"role":"admin"}', now(), now(), '', '', '', ''),
        (v_doctor_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'lekarz@test.pl', crypt('test123!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"role":"doctor"}', now(), now(), '', '', '', ''),
        (v_patient_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'pacjent@test.pl', crypt('test123!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"role":"patient"}', now(), now(), '', '', '', '')
    ON CONFLICT (id) DO UPDATE SET
        encrypted_password = EXCLUDED.encrypted_password,
        email_confirmed_at = EXCLUDED.email_confirmed_at,
        raw_app_meta_data = EXCLUDED.raw_app_meta_data,
        raw_user_meta_data = EXCLUDED.raw_user_meta_data,
        confirmation_token = '',
        recovery_token = '',
        email_change = '',
        email_change_token_new = '';

    INSERT INTO auth.identities (
        id,
        user_id,
        provider_id,
        provider,
        identity_data,
        last_sign_in_at,
        created_at,
        updated_at
    )
    VALUES
        (gen_random_uuid(), v_admin_id, v_admin_id::text, 'email', jsonb_build_object('sub', v_admin_id::text, 'email', 'admin@test.pl'), now(), now(), now()),
        (gen_random_uuid(), v_doctor_id, v_doctor_id::text, 'email', jsonb_build_object('sub', v_doctor_id::text, 'email', 'lekarz@test.pl'), now(), now(), now()),
        (gen_random_uuid(), v_patient_id, v_patient_id::text, 'email', jsonb_build_object('sub', v_patient_id::text, 'email', 'pacjent@test.pl'), now(), now(), now())
    ON CONFLICT (provider, provider_id) DO NOTHING;

    INSERT INTO public.profiles (id, role, first_name, last_name, pesel, phone, address)
    VALUES
        (v_admin_id, 'admin', 'Tomasz', 'Administrator', NULL, '+48 500 000 001', 'ul. Serwerowa 1, Gliwice'),
        (v_doctor_id, 'doctor', 'Jan', 'Kowalski', '75051201234', '+48 500 000 002', 'ul. Lekarska 4, Zabrze'),
        (v_patient_id, 'patient', 'Anna', 'Nowak', '98010112345', '+48 500 000 003', 'ul. Pacjencka 12/3, Katowice')
    ON CONFLICT (id) DO UPDATE SET
        role = EXCLUDED.role,
        first_name = EXCLUDED.first_name,
        last_name = EXCLUDED.last_name,
        pesel = EXCLUDED.pesel,
        phone = EXCLUDED.phone,
        address = EXCLUDED.address;

    INSERT INTO public.clinics (id, name, address, latitude, longitude, phone)
    VALUES (v_clinic_id, 'Centrum Medyczne Politechniki Śląskiej', 'ul. Akademicka 16, 44-100 Gliwice', 50.2885, 18.6775, '+48 32 237 10 00');

    INSERT INTO public.doctor_patients (doctor_id, patient_id)
    VALUES (v_doctor_id, v_patient_id);

    INSERT INTO public.appointments (id, patient_id, doctor_id, clinic_id, scheduled_at, status, notes)
    VALUES (v_appointment_id, v_patient_id, v_doctor_id, v_clinic_id, now() + interval '2 days', 'scheduled', 'Konsultacja internistyczna i przegląd badań');

    INSERT INTO public.medical_records (id, patient_id, doctor_id, appointment_id, record_type, description)
    VALUES (v_record_id, v_patient_id, v_doctor_id, v_appointment_id, 'visit', 'Pacjent zgłasza osłabienie. Zalecono wykonanie morfologii oraz odpoczynek.');

    INSERT INTO public.prescriptions (patient_id, doctor_id, medical_record_id, code, medications, issued_at, valid_until, status)
    VALUES (v_patient_id, v_doctor_id, v_record_id, '1234', 'Amotaks 500mg - 2x dziennie przez 7 dni', now(), current_date + interval '30 days', 'active');

    INSERT INTO public.medical_files (patient_id, uploaded_by, medical_record_id, storage_path, file_name, mime_type)
    VALUES (v_patient_id, v_doctor_id, v_record_id, '33333333-3333-3333-3333-333333333333/morfologia_test.pdf', 'morfologia_test.pdf', 'application/pdf');

    INSERT INTO public.audit_logs (actor_id, action, entity_type, entity_id)
    VALUES (v_doctor_id, 'CREATE_RECORD', 'medical_records', v_record_id);
END $$;