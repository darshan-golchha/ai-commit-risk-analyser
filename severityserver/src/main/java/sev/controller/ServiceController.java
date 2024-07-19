package sev.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;

import sev.service.SeverityService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceController {
	
	@Autowired
	private SeverityService service;

    @PostMapping("/upload")
    public ResponseEntity<ObjectNode> handleFileUpload(@RequestBody String jsonData) {
    	ObjectNode response = service.sonarReciever(jsonData);
    	if (response != null) {
    		return ResponseEntity.ok(response);
    	} else {
    		return ResponseEntity.status(200).body(response);
    	}
    }
}
