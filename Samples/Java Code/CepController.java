package com.chefsuite.controller;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.chefsuite.dto.CEPDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class CepController {

	@RequestMapping(value = "/get-cep", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String getCep(@RequestParam("cep") String cep) {

		ObjectMapper mapper = new ObjectMapper();
		// Create a neat value object to hold the URL
		URL url;
		try {
			url = new URL("http://viacep.com.br/ws/" + cep + "/json/");

			// Open a connection(?) on the URL(??) and cast the response(???)
			HttpURLConnection connection = null;
			connection = (HttpURLConnection) url.openConnection();

			// Now it's "open", we can set the request method, headers etc.
			connection.setRequestProperty("accept", "application/json");

			// This line makes the request
			InputStream responseStream = connection.getInputStream();

			// Manually converting the response body InputStream to APOD using Jackson

			CEPDTO _cep = mapper.readValue(responseStream, CEPDTO.class);

			return mapper.writeValueAsString(_cep);

		} catch (Exception e) {

			e.printStackTrace();

			return "[{}]";
		}
	}
	
	@RequestMapping(value = "/get-cep-by-address", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String getCepByAddress(@RequestParam("state") String state, 
			@RequestParam("city") String city, @RequestParam("address") String address) {

		ObjectMapper mapper = new ObjectMapper();
		// Create a neat value object to hold the URL
		URL url;
		try {
			url = new URL("http://viacep.com.br/ws/" + state.toUpperCase() + "/" + city + "/" + address + "/json/");

			// Open a connection(?) on the URL(??) and cast the response(???)
			HttpURLConnection connection = null;
			connection = (HttpURLConnection) url.openConnection();

			// Now it's "open", we can set the request method, headers etc.
			connection.setRequestProperty("accept", "application/json");

			// This line makes the request
			InputStream responseStream = connection.getInputStream();

			// Manually converting the response body InputStream to APOD using Jackson

			CEPDTO[] _cep = mapper.readValue(responseStream, CEPDTO[].class);
			
			return mapper.writeValueAsString(_cep);

		} catch (Exception e) {

			e.printStackTrace();

			return "[{}]";
		}
	}

}