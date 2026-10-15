package com.fastpilot.ui.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The server must answer every screen path with index.html, or reloading a screen gives 404.
 * The paths live in the language packages; vercel.json (deploy) and scripts/serve_web.py (local)
 * list them again, so this keeps the three in step.
 */
class WebRoutesConfigTest {

    private fun routePattern(file: String, find: Regex): Regex {
        val text = File("../$file").readText()
        val slugs = find.find(text)?.groupValues?.get(1) ?: error("no route pattern in $file")
        return Regex("^/($slugs)/?$")
    }

    @Test
    fun vercelAndTheLocalServerServeEveryScreenPath() {
        // Vercel reads "source" with path-to-regexp: a named parameter, "/:route(a|b)". A bare "/(a|b)/?"
        // is rejected ("Unexpected MODIFIER"), which made the whole vercel.json invalid and every deploy fail
        val vercel = routePattern("vercel.json", Regex(""""source": "/:route\(([a-z|-]+)\)""""))
        val local = routePattern("scripts/serve_web.py", Regex("""ROUTES = re\.compile\(r"\^/\(([a-z|-]+)\)/\?\$"\)"""))
        for (path in ALL_PATHS) {
            assertTrue(vercel.matches(path), "vercel.json does not serve $path")
            assertTrue(local.matches(path), "scripts/serve_web.py does not serve $path")
        }
    }
}
