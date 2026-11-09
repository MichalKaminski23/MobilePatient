-- Prywatny bucket na skany medyczne w Supabase Storage.

INSERT INTO storage.buckets (id, name, public)
VALUES ('skany-medyczne', 'skany-medyczne', false)
ON CONFLICT (id) DO NOTHING;