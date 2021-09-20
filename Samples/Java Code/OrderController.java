package com.chefsuite.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.chefsuite.domain.Account;
import com.chefsuite.domain.CTable;
import com.chefsuite.domain.Customer;
import com.chefsuite.domain.FoodInformation;
import com.chefsuite.domain.Order;
import com.chefsuite.dto.FoodInformationDTO;
import com.chefsuite.enums.CTableStatus;
import com.chefsuite.enums.FoodInformationStatus;
import com.chefsuite.enums.OrderPaymentType;
import com.chefsuite.repository.CTableRepository;
import com.chefsuite.repository.CustomerRepository;
import com.chefsuite.repository.FoodRepository;
import com.chefsuite.repository.OrderRepository;
import com.chefsuite.util.DateUtil;
import com.chefsuite.util.TokenGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

@Controller
public class OrderController {

	@Autowired
	private PrincipalController principalController;

	@Autowired
	private FoodRepository foodRepository;

	@Autowired
	private CTableRepository cTableRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private OrderRepository orderRepository;

	@RequestMapping(value = "/add-order", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String getCTables(@RequestParam("username") String username,
			@RequestParam("customerCode") String customerCode, @RequestParam("tableCode") String tableCode,
			@RequestParam("update") Boolean update, @RequestParam("add") Boolean add,
			@RequestParam("observation") String observation, 
			@RequestParam("orderPaymentType") OrderPaymentType orderPaymentType,
			@RequestBody String jsonStr) {
		
		final Account account = principalController.getAdministrationByEmail(username);

		final Customer customer = customerRepository.findByAccountIdAndCode(account.getId(), customerCode);

		CTable cTable = cTableRepository.findByAccountIdAndCode(account.getId(), tableCode);
		
		if (cTable == null) {

			List<String> codes = cTableRepository.findCodes();

			TokenGenerator tokenGenerator = TokenGenerator.getInstance();

			String deliveryTableCode = tokenGenerator.getToken(33);

			while (codes.contains(deliveryTableCode)) {

				deliveryTableCode = tokenGenerator.getToken(33);

			}

			cTable = new CTable(null, deliveryTableCode, new ArrayList<>(), CTableStatus.FREE, account);
			cTable.setDelivery(true);
			cTable.setAccount(account);

			try {
			
				cTable = cTableRepository.save(cTable);
			
			}catch(Exception e) {
				
				return "{\"response\":\"error\"}";
			
			}
			
		}

		ObjectMapper mapper = new ObjectMapper();
		mapper.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));

		List<FoodInformationDTO> pojos = null;

		try {
			pojos = mapJsonToObjectList(new FoodInformationDTO(), jsonStr, FoodInformationDTO.class);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<FoodInformation> foodInformations = new ArrayList<>();

		if (pojos != null) {

			for (FoodInformationDTO fi : pojos) {

				FoodInformation foodInformation = new FoodInformation();

				foodInformation.setFood(foodRepository.findById(fi.getFood().getId()).orElse(null));
				foodInformation.setFoodInformationStatus(FoodInformationStatus.PREPERING_THE_FOOD);
				foodInformation.setQuantity(fi.getQuantity());
				foodInformation.setAccount(account);
				foodInformation.setCTable(cTable);
				foodInformation.setCut(fi.getCut());
				foodInformation.setNote(fi.getNote());

				foodInformations.add(foodInformation);

			}

		}

		StringBuilder result = new StringBuilder("");

		if (!update) {

			if (cTable.getOrders().size() > 0 && !cTable.getStatus().equals(CTableStatus.FREE)) {

				for (Order o : cTable.getOrders()) {

					o.setCustomer(customer);
					o.setObservation(observation);
					o.setOrderPaymentType(orderPaymentType);

					if (!o.getPaid()) {

						foodInformations.forEach(fi -> {

							boolean contains = false;

							for (FoodInformation ofi : o.getFoodInformations()) {

								if (ofi.getFood().equals(fi.getFood())) {

									ofi.setQuantity(ofi.getQuantity() + fi.getQuantity());

									contains = true;

									break;

								}

							}

							if (!contains) {

								fi.setOrder(o);

								o.getFoodInformations().add(fi);

							}

						});

					}

				}

			} else {

				Calendar c1 = DateUtil.getCalendarNow();
				
				final List<Order> _orders = orderRepository.findByDateAndAccountId(account.getId(), c1.get(Calendar.YEAR), c1.get(Calendar.MONTH) + 1, c1.get(Calendar.DAY_OF_MONTH));

				@SuppressWarnings("unlikely-arg-type")
				List<Order> __orders = _orders.stream()
					    .sorted(Comparator.comparingInt(o -> _orders.indexOf(o.getOrderNumber())))
					    .collect(Collectors.toList());
				
				List<Order> orders = __orders;
				
				Integer orderNumber = null;

				boolean containsToday = false;

				for (Order o : orders) {

					Calendar c2 = Calendar.getInstance();
					c2.setTime(o.getDate());

					if (DateUtil.dayEquals(c1, c2)) {

						containsToday = true;
						break;

					}

				}

				if (!containsToday) {

					orderNumber = 1;

				} else {

					orderNumber = orders.get(orders.size() - 1).getOrderNumber() + 1;

				}

				Order order = new Order(null, orderNumber, c1.getTime(), false, false, foodInformations, customer, 
						orderPaymentType,
						cTable, account);

				order.setObservation(observation);

				cTable.getOrders().add(order);

				cTable.setStatus(CTableStatus.OCCUPIED);

			}

			CTable _result = null;
			
			try {
			
				_result = cTableRepository.save(cTable);
			
			}catch(Exception e) {
				
				return "{\"response\":\"error\"}";
			
			}

			result.append(_result.toString());

		} else {

			for (Order o : cTable.getOrders()) {
				
				o.setCustomer(customer);
				o.setObservation(observation);
				o.setOrderPaymentType(orderPaymentType);
				
				if (!o.getPaid()) {

					List<FoodInformation> foodInformationsToBeRemoved = new ArrayList<>();
					List<FoodInformation> orderFoodInformationsToBeRemoved = new ArrayList<>();

					for (FoodInformation ofi : o.getFoodInformations()) {

						boolean contains = false;

						for (FoodInformation fi : foodInformations) {

							if (ofi.getFood().equals(fi.getFood())) {

								if (add) {

									ofi.setQuantity(ofi.getQuantity() + fi.getQuantity());

								} else {

									ofi.setQuantity(fi.getQuantity());

								}

								foodInformationsToBeRemoved.add(fi);

								contains = true;

							}

						}

						if (!contains) {

							orderFoodInformationsToBeRemoved.add(ofi);

						}

					}

					foodInformations.removeAll(foodInformationsToBeRemoved);
					o.getFoodInformations().removeAll(orderFoodInformationsToBeRemoved);

					foodInformations.forEach(fi -> {

						fi.setDate(new Date());
						fi.setOrder(o);

					});

					o.getFoodInformations().addAll(foodInformations);

					CTable _result = null;
					
					try {
					
						cTableRepository.save(cTable);
					
					}catch(Exception e) {
						
						return "{\"response\":\"error\"}";
					
					}
					
					result.append(_result.toString());

					break;

				}

			}

		}

		String response = null;

		if (!result.toString().isEmpty()) {
			response = "success";
		} else {
			response = "error";
		}

		mapper = new ObjectMapper();
		mapper.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));

		try {
			return mapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			return "[{}]";
		}

	}

	@RequestMapping(value = "/get-orders-by-table", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String getOrdersByCTable(@RequestParam("username") String username,
			@RequestParam("tableCode") String tableCode) {

		Account account = principalController.getAdministrationByEmail(username);

		List<Order> orders = orderRepository.findByAccountIdAndTableCodeAndReady(account.getId(), tableCode, false);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));

		try {
			return mapper.writeValueAsString(orders);
		} catch (JsonProcessingException e) {
			return "[{}]";
		}

	}

	@RequestMapping(value = "/get-orders", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String getOrders(@RequestParam("username") String username) {

		Account account = principalController.getAdministrationByEmail(username);

		List<Order> orders = orderRepository.findByAccountIdAndReady(account.getId(), false);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));

		try {
			return mapper.writeValueAsString(orders);
		} catch (JsonProcessingException e) {
			return "[{}]";
		}

	}

	@RequestMapping(value = "/get-orders-not-paid", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String getOrdersNotPaid(@RequestParam("username") String username) {

		Account account = principalController.getAdministrationByEmail(username);

		List<Order> orders = orderRepository.findByAccountIdAndPaid(account.getId(), false);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));

		try {
			return mapper.writeValueAsString(orders);
		} catch (JsonProcessingException e) {
			return "[{}]";
		}

	}

	@RequestMapping(value = "/get-daily-orders", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String getDailyOrders(@RequestParam("username") String username) {

		Account account = principalController.getAdministrationByEmail(username);

		Calendar c = DateUtil.getCalendarNow();

		List<Order> orders = orderRepository.findByDateAndAccountIdAndPaid(account.getId(), c.get(Calendar.YEAR),
				c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), true);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));

		try {
			return mapper.writeValueAsString(orders);
		} catch (JsonProcessingException e) {
			return "[{}]";
		}

	}

	@RequestMapping(value = "/get-daily-paid-orders-price", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Double getDailyPaidOrdersPrice(@RequestParam("username") String username) {

		Account account = principalController.getAdministrationByEmail(username);

		Calendar c = DateUtil.getCalendarNow();

		Double price = orderRepository.findPriceByDateAndAccountIdAndPaid(c.get(Calendar.YEAR),
				c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), account.getId(), true);

		return price;

	}

	@RequestMapping(value = "/get-last-30-days-paid-orders-price", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Double getLast30DaysPaidOrdersPrice(@RequestParam("username") String username) {

		Account account = principalController.getAdministrationByEmail(username);

		Calendar _30daysAgo = DateUtil.getCalendarNow();
		_30daysAgo.add(Calendar.DAY_OF_MONTH, -30);

		Double price = orderRepository.findPriceByAccountIdAndGreaterThanDate(account.getId(), _30daysAgo.getTime());

		return price;

	}

	@RequestMapping(value = "/get-daily-paid-orders-count", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Integer getDailyPaidOrdersCount(@RequestParam("username") String username) {

		Account account = principalController.getAdministrationByEmail(username);

		Calendar c = DateUtil.getCalendarNow();

		Integer count = orderRepository.findCountByDateAndAccountIdAndPaid(c.get(Calendar.YEAR),
				c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), account.getId(), true);

		return count;

	}

	@RequestMapping(value = "/get-last-30-days-paid-orders-count", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Integer getLast30DaysPaidOrdersCount(@RequestParam("username") String username) {

		Account account = principalController.getAdministrationByEmail(username);

		Calendar _30daysAgo = DateUtil.getCalendarNow();
		_30daysAgo.add(Calendar.DAY_OF_MONTH, -30);

		Integer count = orderRepository.findCountByAccountIdAndGreaterThanDate(account.getId(), _30daysAgo.getTime());

		return count;

	}

	protected static <T> List<T> mapJsonToObjectList(T typeDef, String json, Class<?> clazz) throws Exception {
		List<T> list;
		ObjectMapper mapper = new ObjectMapper();
		mapper.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
		TypeFactory t = TypeFactory.defaultInstance();
		list = mapper.readValue(json, t.constructCollectionType(ArrayList.class, clazz));

		return list;
	}

}