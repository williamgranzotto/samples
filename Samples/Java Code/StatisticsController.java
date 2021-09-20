package com.chefsuite.controller;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.chefsuite.domain.Account;
import com.chefsuite.domain.Food;
import com.chefsuite.domain.FoodCategory;
import com.chefsuite.enums.FoodType;
import com.chefsuite.repository.BillToPayRepository;
import com.chefsuite.repository.BillToReceiveRepository;
import com.chefsuite.repository.FoodCategoryRepository;
import com.chefsuite.repository.FoodInformationRepository;
import com.chefsuite.repository.FoodRepository;
import com.chefsuite.repository.OrderRepository;
import com.chefsuite.util.DateUtil;
import com.chefsuite.util.MapUtil;
import com.chefsuite.util.NumberUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class StatisticsController {

	@Autowired
	private PrincipalController principalController;

	@Autowired
	private FoodRepository foodRepository;

	@Autowired
	private FoodCategoryRepository foodCategoryRepository;

	@Autowired
	private FoodInformationRepository foodInformationRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private BillToReceiveRepository billToReceiveRepository;

	@Autowired
	private BillToPayRepository billToPayRepository;

	@RequestMapping(value = "/get-top-foods", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String getTopFoods(@RequestParam("username") String username,
			@RequestParam("foodType") String foodType, @RequestParam("period") String period) {

		Account account = principalController.getAdministrationByEmail(username);

		List<Food> foods = foodRepository.findByAccountIdAndFoodType(account.getId(), FoodType.valueOf(foodType));

		final Map<String, Integer> map = new HashMap<>();

		if (period.equals("ALL")) {

			foods.forEach(f -> {

				Integer result = foodInformationRepository.findQuantityByFoodCodeAndAccountId(f.getCode(),
						account.getId());

				map.put(f.getName(), result != null ? result : 0);

			});

		} else if (period.equals("MONTH")) {

			Calendar now = DateUtil.getCalendarNow();

			int month = now.get(Calendar.MONTH) + 1;

			foods.forEach(f -> {

				Integer result = foodInformationRepository.findMonthQuantityByFoodCodeAndAccountId(f.getCode(), month,
						account.getId());

				map.put(f.getName(), result != null ? result : 0);

			});

		} else if (period.equals("DAY")) {

			Calendar now = DateUtil.getCalendarNow();

			foods.forEach(f -> {

				Integer result = foodInformationRepository.findDayQuantityByFoodCodeAndAccountId(f.getCode(),
						now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH),
						account.getId());

				map.put(f.getName(), result != null ? result : 0);

			});

		}

		Map<String, Integer> sortedMap = MapUtil.sortByValue(map);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));

		try {
			return mapper.writeValueAsString(sortedMap);
		} catch (JsonProcessingException e) {
			return "[{}]";
		}

	}

	@RequestMapping(value = "/get-sales-by-category", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String getSalesByCategory(@RequestParam("username") String username,
			@RequestParam("period") String period) {

		Account account = principalController.getAdministrationByEmail(username);

		List<FoodCategory> foodCategories = foodCategoryRepository.findByAccountId(account.getId());

		final Map<String, Integer> map = new HashMap<>();

		foodCategories.forEach(fc -> {
			if (period.equals("ALL")) {

				Integer result = foodInformationRepository.findCountByFoodCategoryId(fc.getId());

				map.put(fc.getName(), result != null ? result : 0);

			} else if (period.equals("MONTH")) {

				Calendar now = DateUtil.getCalendarNow();

				int month = now.get(Calendar.MONTH) + 1;

				Integer result = foodInformationRepository.findMonthCountByFoodCategoryId(month, fc.getId());

				map.put(fc.getName(), result != null ? result : 0);

			} else if (period.equals("DAY")) {

				Calendar now = DateUtil.getCalendarNow();

				Integer result = foodInformationRepository.findDayCountByFoodCategoryId(now.get(Calendar.YEAR),
						now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), fc.getId());

				map.put(fc.getName(), result != null ? result : 0);

			}
		});

		ObjectMapper mapper = new ObjectMapper();
		mapper.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));

		try {
			return mapper.writeValueAsString(map);
		} catch (JsonProcessingException e) {
			return "[{}]";
		}

	}

	@RequestMapping(value = "/get-gross-income", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String getGrossIncome(@RequestParam("username") String username,
			@RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate,
			@RequestParam("comparisonPeriod") String comparisonPeriod) {

		Account account = principalController.getAdministrationByEmail(username);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MMM");
		SimpleDateFormat sdf3 = new SimpleDateFormat("MMM/yyyy");

		Date _startDate = null, _endDate = null;

		try {

			_startDate = sdf.parse(startDate);
			_endDate = sdf.parse(endDate);

		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		DateTime startDateTime = new DateTime(_startDate.getTime());
		DateTime endDateTime = new DateTime(_endDate.getTime());

		int days = Days.daysBetween(startDateTime, endDateTime).getDays();

		final TreeMap<String, Double> map = new TreeMap<>();
		final TreeMap<String, Double> profitsMap = new TreeMap<>();
		final TreeMap<String, Double> expensesMap = new TreeMap<>();
		final Map<String, Double> comparisonMap = new TreeMap<>();
		final TreeMap<String, Double> profitsComparisonMap = new TreeMap<>();
		final TreeMap<String, Double> expensesComparisonMap = new TreeMap<>();

		final boolean comparison = comparisonPeriod != null && !StringUtils.isBlank(comparisonPeriod) ? true : false;

		Calendar startDateCalendar = Calendar.getInstance();
		startDateCalendar.setTime(_startDate);

		Calendar endDateCalendar = Calendar.getInstance();
		endDateCalendar.setTime(_endDate);

		Calendar comparisonCalendar = Calendar.getInstance();
		comparisonCalendar.setTime(_startDate);

		switch (comparisonPeriod) {

		case "oneWeekAgo":

			comparisonCalendar.add(Calendar.DAY_OF_MONTH, -7);

			break;
		case "oneMonthAgo":

			comparisonCalendar.add(Calendar.MONTH, -1);

			break;
		case "threeMonthsAgo":

			comparisonCalendar.add(Calendar.MONTH, -3);

			break;
		case "sixMonthsAgo":

			comparisonCalendar.add(Calendar.MONTH, -6);

			break;
		case "oneYearAgo":

			comparisonCalendar.add(Calendar.YEAR, -1);

			break;

		}

		if (days <= 31) {

			endDateCalendar.add(Calendar.DAY_OF_MONTH, 1);

			while (!DateUtil.dayEquals(startDateCalendar, endDateCalendar)) {

				Double result = orderRepository.findPriceByAccountIdAndDateEquals(account.getId(),
						startDateCalendar.get(Calendar.YEAR), startDateCalendar.get(Calendar.MONTH) + 1,
						startDateCalendar.get(Calendar.DAY_OF_MONTH));

				Double profitsResult = billToReceiveRepository.findPriceByAccountIdAndDateEquals(account.getId(),
						startDateCalendar.get(Calendar.YEAR), startDateCalendar.get(Calendar.MONTH) + 1,
						startDateCalendar.get(Calendar.DAY_OF_MONTH));

				Double expensesResult = billToPayRepository.findPriceByAccountIdAndDateEquals(account.getId(),
						startDateCalendar.get(Calendar.YEAR), startDateCalendar.get(Calendar.MONTH) + 1,
						startDateCalendar.get(Calendar.DAY_OF_MONTH));

				if (result == null) {

					result = 0.0d;

				}

				if (profitsResult == null) {

					profitsResult = 0.0d;

				}

				map.put(sdf2.format(startDateCalendar.getTime()), result != null ? result : 0);

				profitsMap.put(sdf2.format(startDateCalendar.getTime()), NumberUtil.round(result + profitsResult, 2));

				expensesMap.put(sdf2.format(startDateCalendar.getTime()), expensesResult != null ? expensesResult : 0);

				if (comparison) {

					result = orderRepository.findPriceByAccountIdAndDateEquals(account.getId(),
							startDateCalendar.get(Calendar.YEAR), startDateCalendar.get(Calendar.MONTH) + 1,
							startDateCalendar.get(Calendar.DAY_OF_MONTH));

					profitsResult = billToReceiveRepository.findPriceByAccountIdAndDateEquals(account.getId(),
							startDateCalendar.get(Calendar.YEAR), startDateCalendar.get(Calendar.MONTH) + 1,
							startDateCalendar.get(Calendar.DAY_OF_MONTH));

					expensesResult = billToPayRepository.findPriceByAccountIdAndDateEquals(account.getId(),
							startDateCalendar.get(Calendar.YEAR), startDateCalendar.get(Calendar.MONTH) + 1,
							startDateCalendar.get(Calendar.DAY_OF_MONTH));

					if (result == null) {

						result = 0.0d;

					}

					if (profitsResult == null) {

						profitsResult = 0.0d;

					}

					profitsComparisonMap.put(sdf2.format(startDateCalendar.getTime()),
							NumberUtil.round(result + profitsResult, 2));

					expensesComparisonMap.put(sdf2.format(startDateCalendar.getTime()),
							expensesResult != null ? expensesResult : 0);

					comparisonCalendar.add(Calendar.DAY_OF_MONTH, 1);

				}

				startDateCalendar.add(Calendar.DAY_OF_MONTH, 1);

			}

		} else {

			endDateCalendar.add(Calendar.MONTH, 1);

			while (!DateUtil.monthEquals(startDateCalendar, endDateCalendar)) {

				Double result = orderRepository.findPriceByAccountIdAndYearAndMonth(account.getId(),
						startDateCalendar.get(Calendar.YEAR), startDateCalendar.get(Calendar.MONTH) + 1);

				Double profitsResult = billToReceiveRepository.findPriceByAccountIdAndYearAndMonth(account.getId(),
						startDateCalendar.get(Calendar.YEAR), startDateCalendar.get(Calendar.MONTH) + 1);

				Double expensesResult = billToPayRepository.findPriceByAccountIdAndYearAndMonth(account.getId(),
						startDateCalendar.get(Calendar.YEAR), startDateCalendar.get(Calendar.MONTH) + 1);

				if (result == null) {

					result = 0.0d;

				}

				if (profitsResult == null) {

					profitsResult = 0.0d;

				}

				map.put(sdf3.format(startDateCalendar.getTime()), result != null ? result : 0);

				profitsMap.put(sdf3.format(startDateCalendar.getTime()),
						new BigDecimal(result + profitsResult).setScale(2).doubleValue());

				expensesMap.put(sdf3.format(startDateCalendar.getTime()), expensesResult != null ? expensesResult : 0);

				startDateCalendar.add(Calendar.MONTH, 1);

				if (comparison) {

					result = orderRepository.findPriceByAccountIdAndYearAndMonth(account.getId(),
							comparisonCalendar.get(Calendar.YEAR), comparisonCalendar.get(Calendar.MONTH) + 1);

					profitsResult = billToReceiveRepository.findPriceByAccountIdAndYearAndMonth(account.getId(),
							comparisonCalendar.get(Calendar.YEAR), comparisonCalendar.get(Calendar.MONTH) + 1);

					expensesResult = billToPayRepository.findPriceByAccountIdAndYearAndMonth(account.getId(),
							comparisonCalendar.get(Calendar.YEAR), comparisonCalendar.get(Calendar.MONTH) + 1);

					if (result == null) {

						result = 0.0d;

					}

					if (profitsResult == null) {

						profitsResult = 0.0d;

					}

					comparisonMap.put(sdf3.format(comparisonCalendar.getTime()), result != null ? result : 0);

					profitsComparisonMap.put(sdf2.format(startDateCalendar.getTime()),
							new BigDecimal(result + profitsResult).setScale(2).doubleValue());

					expensesComparisonMap.put(sdf2.format(startDateCalendar.getTime()),
							expensesResult != null ? expensesResult : 0);

					comparisonCalendar.add(Calendar.MONTH, 1);

				}

			}

		}

		List<Map<String, Double>> maps = Arrays.asList(map, comparisonMap, profitsMap, profitsComparisonMap,
				expensesMap, expensesComparisonMap);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));

		try {
			return mapper.writeValueAsString(maps);
		} catch (JsonProcessingException e) {
			return "[{}]";
		}

	}

	@RequestMapping(value = "/get-average-ticket", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String getAverageTicket(@RequestParam("username") String username,
			@RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate,
			@RequestParam("comparisonPeriod") String comparisonPeriod) {

		Account account = principalController.getAdministrationByEmail(username);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MMM");
		SimpleDateFormat sdf3 = new SimpleDateFormat("MMM/yyyy");

		Date _startDate = null, _endDate = null;

		try {

			_startDate = sdf.parse(startDate);
			_endDate = sdf.parse(endDate);

		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		DateTime startDateTime = new DateTime(_startDate.getTime());
		DateTime endDateTime = new DateTime(_endDate.getTime());

		int days = Days.daysBetween(startDateTime, endDateTime).getDays();

		final TreeMap<String, Double> map = new TreeMap<>();
		final Map<String, Double> comparisonMap = new TreeMap<>();

		final boolean comparison = comparisonPeriod != null && !StringUtils.isBlank(comparisonPeriod) ? true : false;

		Calendar startDateCalendar = Calendar.getInstance();
		startDateCalendar.setTime(_startDate);

		Calendar endDateCalendar = Calendar.getInstance();
		endDateCalendar.setTime(_endDate);

		Calendar comparisonCalendar = Calendar.getInstance();
		comparisonCalendar.setTime(_startDate);

		switch (comparisonPeriod) {

		case "oneWeekAgo":

			comparisonCalendar.add(Calendar.DAY_OF_MONTH, -7);

			break;
		case "oneMonthAgo":

			comparisonCalendar.add(Calendar.MONTH, -1);

			break;
		case "threeMonthsAgo":

			comparisonCalendar.add(Calendar.MONTH, -3);

			break;
		case "sixMonthsAgo":

			comparisonCalendar.add(Calendar.MONTH, -6);

			break;
		case "oneYearAgo":

			comparisonCalendar.add(Calendar.YEAR, -1);

			break;

		}

		if (days <= 31) {

			endDateCalendar.add(Calendar.DAY_OF_MONTH, 1);

			while (!DateUtil.dayEquals(startDateCalendar, endDateCalendar)) {

				Double result = this.calcAverageTicket(account, startDateCalendar);

				map.put(sdf2.format(startDateCalendar.getTime()), result);

				if (comparison) {

					result = this.calcAverageTicket(account, comparisonCalendar);

					comparisonMap.put(sdf2.format(comparisonCalendar.getTime()), result);

					comparisonCalendar.add(Calendar.DAY_OF_MONTH, 1);

				}

				startDateCalendar.add(Calendar.DAY_OF_MONTH, 1);

			}

		} else {

			endDateCalendar.add(Calendar.MONTH, 1);

			while (!DateUtil.monthEquals(startDateCalendar, endDateCalendar)) {

				Double result = this.calcAverageTicketByYearAndMonth(account, startDateCalendar.get(Calendar.YEAR),
						startDateCalendar.get(Calendar.MONTH) + 1);

				map.put(sdf3.format(startDateCalendar.getTime()), result);

				startDateCalendar.add(Calendar.MONTH, 1);

				if (comparison) {

					result = this.calcAverageTicketByYearAndMonth(account, comparisonCalendar.get(Calendar.YEAR),
							comparisonCalendar.get(Calendar.MONTH) + 1);

					comparisonMap.put(sdf3.format(comparisonCalendar.getTime()), result);

					comparisonCalendar.add(Calendar.MONTH, 1);

				}

			}

		}

		List<Map<String, Double>> maps = Arrays.asList(map, comparisonMap);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));

		try {
			return mapper.writeValueAsString(maps);
		} catch (JsonProcessingException e) {
			return "[{}]";
		}

	}

	private Double calcAverageTicket(Account account, Calendar c) {

		Double price = orderRepository.findPriceByAccountIdAndDateEquals(account.getId(), c.get(Calendar.YEAR),
				c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));

		Integer count = orderRepository.findCountByAccountIdAndDateEquals(account.getId(), c.get(Calendar.YEAR),
				c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));

		if (price == null) {

			price = 0.0d;

		}

		if (count == null) {

			count = 0;

		}

		return NumberUtil.round(price / count, 2);

	}

	private Double calcAverageTicketByYearAndMonth(Account account, int year, int month) {

		Double price = orderRepository.findPriceByAccountIdAndYearAndMonth(account.getId(), year, month);

		Integer count = orderRepository.findCountByAccountIdAndYearAndMonth(account.getId(), year, month);

		if (price == null) {

			price = 0.0d;

		}

		if (count == null) {

			count = 0;

		}

		return NumberUtil.round(price / count, 2);

	}

	private Double calcAverageTicketGreaterThanDate(Account account, Date paymentDate) {

		Double price = orderRepository.findPriceByAccountIdAndGreaterThanDate(account.getId(), paymentDate);

		Integer count = orderRepository.findCountByAccountIdAndGreaterThanDate(account.getId(), paymentDate);

		if (price == null) {

			price = 0.0d;

		}

		if (count == null) {

			count = 0;

		}

		return NumberUtil.round(price / count, 2);

	}

	@RequestMapping(value = "/get-daily-average-ticket-price", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Double getDailyAverageTicketPrice(@RequestParam("username") String username) {

		Account account = principalController.getAdministrationByEmail(username);

		Calendar c = DateUtil.getCalendarNow();

		Double price = this.calcAverageTicket(account, c);

		return price;

	}

	@RequestMapping(value = "/get-last-30-days-average-ticket-price", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Double getLast30DaysAverageTicketPrice(@RequestParam("username") String username) {

		Account account = principalController.getAdministrationByEmail(username);

		Calendar c = DateUtil.getCalendarNow();
		c.add(Calendar.DAY_OF_MONTH, -30);

		Double price = this.calcAverageTicketGreaterThanDate(account, c.getTime());

		return price;

	}

}