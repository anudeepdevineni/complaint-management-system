package com.project.complaintsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the bundled React single-page app.
 *
 * The frontend is built (npm run build) and its files live in
 * src/main/resources/static/, so Spring Boot serves index.html at "/"
 * and the JS/CSS under /assets/** automatically.
 *
 * The problem this class solves: React Router handles routes like /admin or
 * /login in the browser. If a user refreshes or deep-links to /admin, the
 * browser asks the SERVER for /admin — which is not a real file, so Spring
 * would return 404. We forward those known client routes to index.html and let
 * React Router take over. API calls (/api/**) and static files are untouched.
 */
@Controller
public class SpaController {

    @GetMapping({ "/login", "/signup", "/student", "/staff", "/admin" })
    public String spaRoutes() {
        return "forward:/index.html";
    }
}
