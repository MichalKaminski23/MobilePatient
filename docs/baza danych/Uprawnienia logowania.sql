alter table public.profiles enable row level security;

drop policy if exists "Users can read their own profile"
on public.profiles;

create policy "Users can read their own profile"
on public.profiles
for select
to authenticated
using ((select auth.uid()) = id);

select
    policyname,
    cmd,
    roles,
    qual
from pg_policies
where schemaname = 'public'
  and tablename = 'profiles';