package vn.admin.controller;

// Logger not needed while controller is a placeholder for admin endpoints
import org.springframework.web.bind.annotation.RestController;

@RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/map")
public class AdminController {

    // logger removed because controller currently has no active handlers; add when needed

    // private final DistrictRepository districtRepository;

    // @Autowired
    // private DataImporter dataImporter;

    // AdminController(DistrictRepository districtRepository) {
    //     this.districtRepository = districtRepository;
    // }

    // @GetMapping("/districts")
    // public List<?> getAllDistricts() {
    //     logger.info("Fetching all districts");

    //     return districtRepository.findAll();
    // }

}