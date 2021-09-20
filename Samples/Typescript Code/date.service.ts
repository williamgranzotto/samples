import { Injectable} from '@angular/core'
import { FormGroup } from '@angular/forms';
import { formatDate } from '@angular/common';

@Injectable({
  providedIn: 'root',
})
export class DateService {

  constructor() {

  }

  public toDate(dateStr) {

    var parts = dateStr.split("-")
    return new Date(parts[0], parts[1] - 1, parts[2])

  }

  public millisToDate(millis) {

    let date: Date = new Date();
    date.setTime(millis);

    return formatDate(date, "dd/MM/yyyy", "en");

  }

  public millisToDatetime(millis) {

    let date: Date = new Date();
    date.setTime(millis);

    return formatDate(date, "dd/MM/yyyy HH:mm", "en");

  }

  public dateToDatetime(date: Date) {

    return formatDate(date, "dd/MM/yyyy HH:mm", "en");

  }

  public millisToTime(millis) {

    let date: Date = new Date();
    date.setTime(millis);

    return formatDate(date, "HH:mm", "en");

  }

  public dateToString(date) {

      var mm = date.getMonth() + 1;
      var dd = date.getDate();

      return [date.getFullYear(),
      (mm > 9 ? '' : '0') + mm,
      (dd > 9 ? '' : '0') + dd
      ].join('-');
    
  }

  public dateDifferenceInMinutes(dateMiliseconds: number) {

    let today = new Date();

    let millis = <number><unknown>today.getTime() - dateMiliseconds;
    var minutes = Math.floor(millis / 60000)
    
    return minutes;

  }

}
