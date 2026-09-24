package com.atm.management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({
            "/",
            "/login",
            "/register",
            "/reset-pin",
            "/dashboard",
            "/cash",
            "/transfer",
            "/statement",
            "/change-pin",
            "/admin",
            "/audit",
            "/deposit",
            "/withdraw",
            "/fast-cash",
            "/beneficiaries",
            "/balance"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
