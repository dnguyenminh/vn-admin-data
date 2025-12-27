package vn.admin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

@RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/map")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

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