package com.example.pullit.auth

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseManager {
    val client = createSupabaseClient(
        supabaseUrl = "https://boeegnlelfkxkucbnxjm.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJvZWVnbmxlbGZreGt1Y2JueGptIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIzNTM3MDAsImV4cCI6MjA4NzkyOTcwMH0.iLVAAee-AfifG4_EG2Ex44Z82J3h5UDMxR818gvO4tA"
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}
