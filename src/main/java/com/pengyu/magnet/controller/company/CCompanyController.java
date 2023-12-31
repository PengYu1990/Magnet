package com.pengyu.magnet.controller.company;

import com.pengyu.magnet.config.CONSTANTS;
import com.pengyu.magnet.dto.CompanyRequest;
import com.pengyu.magnet.dto.CompanyResponse;
import com.pengyu.magnet.service.compnay.CompanyService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ccompanies")
public class CCompanyController {
    private final CompanyService companyService;

    /**
     * Save company info
     * @param companyRequest
     * @return
     */
    @RolesAllowed({CONSTANTS.ROLE_COMPANY})
    @PostMapping
    public CompanyResponse save(@Valid @RequestBody CompanyRequest companyRequest){
        return companyService.save(companyRequest);
    }

    @RolesAllowed({CONSTANTS.ROLE_COMPANY})
    @GetMapping
    public CompanyResponse find(){
        return companyService.findCurrentCompany();
    }
}
