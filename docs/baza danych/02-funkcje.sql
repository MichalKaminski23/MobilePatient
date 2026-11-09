-- Automatyczne tworzenie profilu po utworzeniu użytkownika w Supabase Auth.

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

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();