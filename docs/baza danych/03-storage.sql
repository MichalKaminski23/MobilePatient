-- Prywatny bucket na skany medyczne w Supabase Storage.

INSERT INTO storage.buckets (id, name, public)
VALUES ('medical-scans', 'medical-scans', false)
ON CONFLICT (id) DO NOTHING;