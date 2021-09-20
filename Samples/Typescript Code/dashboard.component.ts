import { Component, OnInit } from '@angular/core';
import { faEllipsisV } from '@fortawesome/free-solid-svg-icons';
import { GlobalService } from '../services/global.service';
import { HttpClient } from '@angular/common/http';
import { CurrencyService } from '../services/currency.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  faEllipsisV = faEllipsisV;

  earningsToday: string;

  earningsLast30Days: string;

  ordersToday: string;

  ordersLast30Days: string;

  foodsToday: string;

  foodsLast30Days: string;

  drinksToday: string;

  drinksLast30Days: string;

  averageTicketToday: string;

  averageTicketLast30Days: string;

  username: string;

  constructor(public httpClient: HttpClient,
    public globalService: GlobalService,
    public currencyService: CurrencyService) { }

  ngOnInit(): void {

    const tokenPayload = atob(localStorage.getItem('token'));
    this.username = tokenPayload.split(':')[2];

    this.getDailyPaidOrdersPrice();
    this.getlast30DaysPaidOrdersPrice();

    this.getDailyPaidOrdersCount();
    this.getlast30DaysPaidOrdersCount();

    this.getDailyPaidFoodsCount();
    this.getlast30DaysPaidFoodsCount();

    this.getDailyPaidDrinksCount();
    this.getlast30DaysPaidDrinksCount();

    this.getDailyAverageTicketPrice();
    this.getlast30DaysAverageTicketPrice();

  }

  private getDailyPaidOrdersPrice() {

    const tokenPayload = atob(localStorage.getItem('token'));
    const result = tokenPayload.split(':')[0];

    this.httpClient.get(this.globalService.url + '/get-daily-paid-orders-price/?username=' + result).subscribe((response) => {

      this.earningsToday = CurrencyService.formatNumber(<number>response);
      
    });

  }

  private getlast30DaysPaidOrdersPrice() {

    const tokenPayload = atob(localStorage.getItem('token'));
    const result = tokenPayload.split(':')[0];

    this.httpClient.get(this.globalService.url + '/get-last-30-days-paid-orders-price/?username=' + result).subscribe((response) => {

      this.earningsLast30Days = CurrencyService.formatNumber(<number>response);

    });

  }

  private getDailyPaidOrdersCount() {

    const tokenPayload = atob(localStorage.getItem('token'));
    const result = tokenPayload.split(':')[0];

    this.httpClient.get(this.globalService.url + '/get-daily-paid-orders-count/?username=' + result).subscribe((response) => {

      this.ordersToday = <string>response;

    });

  }

  private getlast30DaysPaidOrdersCount() {

    const tokenPayload = atob(localStorage.getItem('token'));
    const result = tokenPayload.split(':')[0];

    this.httpClient.get(this.globalService.url + '/get-last-30-days-paid-orders-count/?username=' + result).subscribe((response) => {

      this.ordersLast30Days = <string>response;

    });

  }

  private getDailyPaidFoodsCount() {

    const tokenPayload = atob(localStorage.getItem('token'));
    const result = tokenPayload.split(':')[0];

    this.httpClient.get(this.globalService.url + '/get-daily-paid-foods-count/?username=' + result).subscribe((response) => {

      this.foodsToday = <string>response;

    });

  }

  private getlast30DaysPaidFoodsCount() {

    const tokenPayload = atob(localStorage.getItem('token'));
    const result = tokenPayload.split(':')[0];

    this.httpClient.get(this.globalService.url + '/get-last-30-days-paid-foods-count/?username=' + result).subscribe((response) => {

      this.foodsLast30Days = <string>response;

    });

  }

  private getDailyPaidDrinksCount() {

    const tokenPayload = atob(localStorage.getItem('token'));
    const result = tokenPayload.split(':')[0];

    this.httpClient.get(this.globalService.url + '/get-daily-paid-drinks-count/?username=' + result).subscribe((response) => {

      this.drinksToday = <string>response;

    });

  }

  private getlast30DaysPaidDrinksCount() {

    const tokenPayload = atob(localStorage.getItem('token'));
    const result = tokenPayload.split(':')[0];

    this.httpClient.get(this.globalService.url + '/get-last-30-days-paid-drinks-count/?username=' + result).subscribe((response) => {

      this.drinksLast30Days = <string>response;

    });

  }

  private getDailyAverageTicketPrice() {

    const tokenPayload = atob(localStorage.getItem('token'));
    const result = tokenPayload.split(':')[0];

    this.httpClient.get(this.globalService.url + '/get-daily-average-ticket-price/?username=' + result).subscribe((response) => {

      this.averageTicketToday = CurrencyService.formatNumber(<number>response);

    });

  }

  private getlast30DaysAverageTicketPrice() {

    const tokenPayload = atob(localStorage.getItem('token'));
    const result = tokenPayload.split(':')[0];

    this.httpClient.get(this.globalService.url + '/get-last-30-days-average-ticket-price/?username=' + result).subscribe((response) => {

      this.averageTicketLast30Days = CurrencyService.formatNumber(<number>response);

    });

  }



}
