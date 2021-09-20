package com.chefsuite.controller;

import java.io.StringReader;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.chefsuite.domain.User;
import com.chefsuite.dto.UserCreditCardDetailsDTO;
import com.chefsuite.errors.RestTemplateResponseErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class PagSeguroController {

	@Value("${sandbox}")
	private Boolean sandbox = true;
	
	private String token = "1C21E2529B594DA68C05811269317618";
	
	private String senderHash;
	
	@Autowired
	private PrincipalController principalController;
	
	@RequestMapping(value = "/pagseguro/session", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String getSession() {

		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("email", "williamgranzotto@gmail.com");
		map.add("token", token);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

		ResponseEntity<String> response = restTemplate.postForEntity("https://ws." + (sandbox ? "sandbox." : "") + "pagseguro.uol.com.br/v2/sessions",
				request, String.class);

		String id = "";
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(response.getBody()));
			Document document = documentBuilder.parse(is);
			id = document.getElementsByTagName("id").item(0).getTextContent();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "{\"id\":\"" + id + "\"}";
	}
	
	@RequestMapping(value = "/pagseguro/creditcard", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String getCreditCard(@RequestBody String jsonStr) {
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));

		UserCreditCardDetailsDTO userCreditCard = null;

		try {
			userCreditCard = mapper.readValue(jsonStr, UserCreditCardDetailsDTO.class);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		User user = principalController.getUserByEmail(userCreditCard.getEmail());

		RestTemplateBuilder builder = new RestTemplateBuilder();
		RestTemplate restTemplate = builder.errorHandler(new RestTemplateResponseErrorHandler()).build();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();

		String reference = "";
		
		map.add("email", "williamgranzotto@gmail.com");
		map.add("token", token);
		
		map.add("paymentMode", "default");
		map.add("paymentMethod", "creditCard");
		map.add("receiverEmail", "williamgranzotto@gmail.com");
		map.add("currency", "BRL");
		map.add("extraAmount", "0.00");
		map.add("itemId1", "default_value");
		map.add("itemDescription1", "default_value");
		map.add("itemAmount1", userCreditCard.getAmount());
		map.add("itemQuantity1", "1");
		map.add("notificationURL", "https://app.chefsuite.com.br/notificationURL?ref=" + reference);
		map.add("reference", reference);
		map.add("senderName", userCreditCard.getUserName());
		map.add("senderCPF", "06045818903");
		map.add("senderAreaCode", "44");
		map.add("senderPhone", "999234915");
		map.add("senderEmail", userCreditCard.getEmail());
		map.add("senderHash", senderHash);
		map.add("creditCardToken", userCreditCard.getCreditCardToken());
		map.add("installmentQuantity", "1");
		map.add("installmentValue", userCreditCard.getAmount());
		map.add("noInterestInstallmentQuantity", "2");
		
		map.add("creditCardHolderName", user.getName());
		map.add("creditCardHolderCPF", "06045818903");
		map.add("creditCardHolderBirthDate", "14/10/1985");
		map.add("creditCardHolderAreaCode", "44");
		map.add("creditCardHolderPhone", "999234915");
		
		map.add("shippingAddressRequired", "false");
		map.add("billingAddressStreet", "Rua Epitácio Pessoa");
		map.add("billingAddressNumber", "64");
		map.add("billingAddressComplement", "");
		map.add("billingAddressDistrict", "PR");
		map.add("billingAddressPostalCode", "87200005");
		map.add("billingAddressCity", "Cianorte");
		map.add("billingAddressState", "PR");
		map.add("billingAddressCountry", "BRA");

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
		try {

			restTemplate.postForEntity("https://ws." + (sandbox ? "sandbox." : "") + "pagseguro.uol.com.br/v2/transactions", request, String.class);

		} catch (Exception e) {
			
			return "{\"response\":\"error\"}";
		
		}
		

		return "{\"response\":\"success\"}";
	}
	
	@RequestMapping(value = "/pagseguro/boletoLink", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public @ResponseBody String getBoletoLink(@RequestParam("username") String username, @RequestParam("amount") String amount,
			@RequestParam("cpf") String cpf, @RequestParam("hash") String hash) {
		
		User user = principalController.getUserByEmail(username);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		String ref = "";

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("email", "williamgranzotto@gmail.com");
		map.add("token", token);
		
		map.add("paymentMode", "default");
		map.add("paymentMethod", "boleto");
		map.add("receiverEmail", "williamgranzotto@gmail.com");
		map.add("currency", "BRL");
		map.add("extraAmount", "0.00");
		map.add("itemId1", "default_value");
		map.add("itemDescription1", "default_value");
		map.add("itemAmount1", amount);
		map.add("itemQuantity1", "1");
		map.add("notificationURL", "https://app.chefsuite.com.br/pagseguro/notificationURL?ref=" + ref);
		map.add("reference", ref);
		map.add("senderName", user.getName());
		map.add("senderCPF", cpf);
		map.add("senderAreaCode", "44");
		map.add("senderPhone", "999234915");
		map.add("senderEmail", "william@sandbox.pagseguro.com.br");
		map.add("senderHash", hash);
		map.add("shippingAddressRequired", "false");

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

		RestTemplateBuilder builder = new RestTemplateBuilder();

		RestTemplate restTemplate2 = builder.errorHandler(new RestTemplateResponseErrorHandler()).build();

		ResponseEntity<String> response = restTemplate2.postForEntity("https://ws." + (sandbox ? "sandbox." : "") + "pagseguro.uol.com.br/v2/transactions",
				request, String.class);

		String paymentLink = "";
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(response.getBody()));
			Document document = documentBuilder.parse(is);
			paymentLink = document.getElementsByTagName("paymentLink").item(0).getTextContent();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "{\"paymentLink\":\"" + paymentLink + "\"}";
	}

}