package com.chefsuite.controller;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.chefsuite.domain.Account;
import com.chefsuite.domain.Code;
import com.chefsuite.domain.Customer;
import com.chefsuite.domain.User;
import com.chefsuite.domain.WhatsappMessage;
import com.chefsuite.dto.CustomerDTO;
import com.chefsuite.dto.WhatsappMessageDTO;
import com.chefsuite.enums.WhatsappMessageType;
import com.chefsuite.repository.AccountRepository;
import com.chefsuite.repository.CodeRepository;
import com.chefsuite.repository.CustomerRepository;
import com.chefsuite.repository.UserRepository;
import com.chefsuite.repository.WhatsappMessageRepository;
import com.chefsuite.util.CodeUtil;
import com.chefsuite.util.DateUtil;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@EnableWebMvc
public class ChatController {

	@Autowired
	PrincipalController principalController;

	@Autowired
	NavigationController navigationController;

	@Autowired
	WhatsappMessageRepository whatsappMessageRepository;

	@Autowired
	CustomerRepository customerRepository;

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	CodeRepository codeRepository;

	@MessageMapping("/chat/{roomName}")
	@SendTo("/topic/messages/{roomName}")
	public WhatsappMessageDTO send(WhatsappMessageDTO message, @DestinationVariable String roomName) throws Exception {

		if (message.getWhatsappMessageType().equals(WhatsappMessageType.READY)) {

			User user = userRepository.findByEmail(message.getFrom());

			Account account = user.getAccount();

			account.setWhatsappImageUrl(message.getWhatsappImageUrl());

			account.setWhatsappPushname(
					message.getWhatsappPushname() != null && !message.getWhatsappPushname().trim().isEmpty()
							? message.getWhatsappPushname()
							: null);

			accountRepository.save(account);

		}

		if (message.getWhatsappMessageType().equals(WhatsappMessageType.QRCODE)
				|| message.getWhatsappMessageType().equals(WhatsappMessageType.READY)
				|| message.getWhatsappMessageType().equals(WhatsappMessageType.LOAD_CUSTOMERS)
				|| message.getWhatsappMessageType().equals(WhatsappMessageType.READ)
				|| message.getWhatsappMessageType().equals(WhatsappMessageType.LOGOUT)) {

			return message;

		}

		User user = userRepository.findByEmail(message.getFrom());

		Account account = user.getAccount();

		if (message.getWhatsappMessageType().equals(WhatsappMessageType.SAVE_CUSTOMERS)) {

			ObjectMapper mapper = new ObjectMapper();

			try {
				CustomerDTO[] customers = mapper.readValue(message.getContactsJson().replaceAll("'", "\""),
						CustomerDTO[].class);

				List<String> numbers = customerRepository.findWhatsappNumbersAccountId(account.getId());

				for (CustomerDTO customer : customers) {

					Customer _customer = null;

					if (numbers != null && customer.getContact().getNumber() != null
							&& !customer.getContact().getNumber().trim().isEmpty()) {

						_customer = customerRepository.findByAccountIdAndWhatsappNumber(account.getId(),
								customer.getContact().getNumber());

					}

					if (_customer == null && !message.getWhatsappImageUrl().equals(account.getWhatsappImageUrl())) {

						_customer = new Customer();
						_customer.setAccount(account);

						Code code = codeRepository.findByAccountId(account.getId());

						_customer.setCode(CodeUtil.getCodeOneHundred(code.getCustomerCode(), "C"));

					}

					if (_customer != null) {

						_customer.setName(customer != null
								? customer.getContact() != null ? customer.getContact().getPushname() : ""
								: "");
						_customer.setWhatsappNumber(customer.getContact().getNumber());
						_customer.setWhatsappImageUrl(customer.getContact().getPic());
						_customer.setIsGroup(customer.getContact().getIsGroup());
						_customer.setIsWAContact(customer.getContact().getIsWAContact());

						customerRepository.save(_customer);

					}

				}

				System.out.println(customers);
			} catch (JsonParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return message;

		}

		WhatsappMessage whatsappMessage = null;

		whatsappMessage = new WhatsappMessage(null, message.getFrom() + "-" + message.getTo(), message.getTo(),
				message.getFrom(), message.getMessage(), new Date(), message.getWhatsappMessageType(),
				message.getBase64Image(), account);

		/**
		 * 
		 * CODE TO SHOW WELCOME MESSAGE
		 * 
		 */

		if (message.getWhatsappMessageType().equals(WhatsappMessageType.INBOUND)) {

			List<WhatsappMessage> whatsappMessages = whatsappMessageRepository.findByAccountIdAndRoom(account.getId(),
					message.getFrom() + "-" + message.getTo());

			WhatsappMessage _whatsappMessage = whatsappMessages != null && whatsappMessages.size() > 0
					? whatsappMessages.get(whatsappMessages.size() - 1)
					: null;

			Calendar now = Calendar.getInstance();
			now.setTime(new Date());

			Calendar wCalendar = Calendar.getInstance();
			wCalendar.setTime(whatsappMessage.getDateNow());

			if (_whatsappMessage == null
					|| (DateUtil.dayEquals(now, wCalendar) && _whatsappMessage.getShowWelcomeMessage())) {

				if (_whatsappMessage != null) {

					_whatsappMessage.setShowWelcomeMessage(false);

					whatsappMessageRepository.save(_whatsappMessage);

				}

				whatsappMessage.setShowWelcomeMessage(false);

				message.setShowWelcomeMessage(true);

			}

		} else {

			whatsappMessage.setShowWelcomeMessage(false);

		}

		/**
		 * 
		 * END OF CODE TO SHOW WELCOME MESSAGE
		 * 
		 */

		whatsappMessage = whatsappMessageRepository.save(whatsappMessage);

		String whatsappNumber = message.getTo();

		Customer customer = customerRepository.findByAccountIdAndWhatsappNumber(account.getId(), whatsappNumber);

		Code code = codeRepository.findByAccountId(account.getId());

		if (customer == null) {

			customer = new Customer();
			customer.setCode(CodeUtil.getCodeOneHundred(code.getCustomerCode(), "C"));
			customer.setAccount(account);
			customer.setWhatsappNumber(whatsappNumber);
			customer.setWhatsappImageUrl(message.getWhatsappImageUrl());

		}

		customer.setLastMessage(message.getMessage());
		customer.setLastMessageIsMe(
				message.getWhatsappMessageType().equals(WhatsappMessageType.OUTBOUND) ? true : false);
		customer.setLastMessageIsPicture(
				message.getBase64Image() == null ? false : !message.getBase64Image().trim().equals(""));
		customer.setLastMessageDate(new Date());
		customer.setMessagesNotViewed(customer.getMessagesNotViewed() + 1);

		customer = customerRepository.save(customer);

		code.setCustomerCode(code.getCustomerCode() + 1);

		codeRepository.save(code);

		return message;
	}

	public static void main(String[] args) {

	}

}