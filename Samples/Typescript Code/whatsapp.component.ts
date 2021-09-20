import { Component, OnInit, ViewChild, Output, EventEmitter } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EmojiButton } from '@joeattardi/emoji-button';
import { faGrinAlt } from '@fortawesome/free-solid-svg-icons';
import { faSearch } from '@fortawesome/free-solid-svg-icons';
import { faPaperPlane } from '@fortawesome/free-solid-svg-icons';
import { faEllipsisV } from '@fortawesome/free-solid-svg-icons';
import { faCheckCircle } from '@fortawesome/free-solid-svg-icons';
import { faArrowLeft } from '@fortawesome/free-solid-svg-icons';
import { faAngleDown } from '@fortawesome/free-solid-svg-icons';
import { faCheckDouble } from '@fortawesome/free-solid-svg-icons';
import { faCommentDots } from '@fortawesome/free-solid-svg-icons';
import { faCamera } from '@fortawesome/free-solid-svg-icons';
import { GlobalService } from '../services/global.service';
import { Account } from '../dto/Account';
import { DateService } from '../services/date.service';
import * as SockJS from 'sockjs-client';
import { Stomp } from "@stomp/stompjs";
//import { setTimeout } from 'timers';//it was left commented because it's a javascript function
import { Customer } from '../dto/Customer';
import { FormGroup } from '@angular/forms';
import { OrdersComponent } from '../orders/orders.component';
import { WhatsappCustomMessages } from '../dto/WhatsappCustomMessages';
declare var require: any;
import Swal from 'sweetalert2';
import { CurrencyService } from '../services/currency.service';

@Component({
  selector: 'app-whatsapp',
  templateUrl: './whatsapp.component.html',
  styleUrls: ['./whatsapp.component.css']
})
export class WhatsappComponent implements OnInit {

  faGrinAlt = faGrinAlt;
  faSearch = faSearch;
  faPaperPlane = faPaperPlane;
  faEllipsisV = faEllipsisV;
  faCheckCircle = faCheckCircle;
  faArrowLeft = faArrowLeft;
  faAngleDown = faAngleDown;
  faCheckDouble = faCheckDouble;
  faCommentDots = faCommentDots;
  faCamera = faCamera;

  message: string = "";

  messages: Array<any>;

  customers: Array<Customer>;

  companyName: string;

  companyWhatsappNumber: string;

  customerWhatsappNumber: string;

  selectedCustomer: Customer;

  @Output() newItemEvent = new EventEmitter<any>();

  qRCode: any;

  qRCodeBase64: unknown = "";

  displayQRCode: unknown = true;

  to: string = null;

  whatsappImageUrl: string;

  whatsappPushname: string;

  whatsappMessagesRead = new Array();

  displayWhatsappIcon: boolean = false;

  selectedCustomerCheckedMessage: string;

  waLoading: boolean = false;

  socket;

  stompClient;

  showSendMessageButton: boolean = true;

  constructor(public httpClient: HttpClient, public globalService: GlobalService, public dateService: DateService,
    public currencyService: CurrencyService) { }

  ngOnInit(): void {
    
    let outer = this;

    setInterval(function () {

      outer.updateCustomerChecked();

    }, 1000);

    const tokenPayload = atob(localStorage.getItem('token'));
    const result = tokenPayload.split(':')[0];
    
    outer.initSocket(result);
    
    this.httpClient.get(this.globalService.url + '/get-account/?username=' + result).subscribe((response) => {
      
      let account: Account = <Account>response;
      outer.companyName = account.companyName != null && account.companyName != "" ? account.companyName : "Lanchonete 01";
      outer.companyWhatsappNumber = account.whatsappPhone;

      outer.whatsappImageUrl = account.whatsappImageUrl;

      outer.whatsappPushname = account.whatsappPushname;

    });

    let data = JSON.stringify({
      email: result
    })
    
    let _displayQRCode = localStorage.getItem('displayQRCode');
    
    if (_displayQRCode == null || _displayQRCode == undefined || _displayQRCode.trim() == "" ) {

      //outer.logout_();

      setTimeout(function() {
        
        outer.httpClient.post(outer.globalService.url + "/init-whatsapp", data).subscribe((response) => {

          outer.displayQRCode = true;

        });

      }, 2000);
      

    } else {

        outer.displayQRCode = false;

    }

    this.getCustomers();

  }

  ngAfterViewInit(): void {

   
    const picker = new EmojiButton();
    const trigger: HTMLElement = document.querySelector('#emoji-trigger');

    picker.on('emoji', selection => {
      // handle the selected emoji here
      this.message += selection.emoji;
    });

    

    trigger.addEventListener('click', () => {
      picker.togglePicker(trigger)
    });

  }

  scrollDown() {

    var objDiv = document.getElementById("messages");
   
    objDiv.scrollTo(0, objDiv.scrollHeight)

  }

  updateCustomerChecked() {

    if (this.selectedCustomer != null && this.selectedCustomer != undefined) {
      
      if (this.selectedCustomer.checkedDate != null) {
        
        var now = new Date();
        var checkedDate = new Date(this.selectedCustomer.checkedDate)
        
        var dif = now.getTime() - checkedDate.getTime();
        
        var seconds = dif / 1000;

        if (seconds <= 10) {

          this.selectedCustomerCheckedMessage = "Online";

        } else if (checkedDate.getDate() == now.getDate()) {

          this.selectedCustomerCheckedMessage = "Visto por último hoje às " + this.dateService.millisToTime(checkedDate.getTime());

        } else if (checkedDate.getDate() == (now.getDate() - 1)) {

          this.selectedCustomerCheckedMessage = "Visto por último ontem às " + this.dateService.millisToTime(checkedDate.getTime());

        } else {

          this.selectedCustomerCheckedMessage = "Visto por último dia " + this.dateService.millisToDate(checkedDate.getTime()) + " às " + this.dateService.millisToTime(checkedDate.getTime());

        };

      }

    }

  }

  changeZIndex() {

    let emojis = document.getElementsByClassName("emoji-picker__wrapper");
    emojis.item(0)["style"]["z-index"] = 9999;


  }

  sendMessage() {

    let outer = this;

    if (outer.message == null || outer.message == undefined || outer.message.trim() == "") {

      return;

    }

    const tokenPayload = atob(localStorage.getItem('token'));
    const email = tokenPayload.split(':')[0];

   // let socket = new SockJS(this.globalService.url + '/chat');

    //let stompClient = Stomp.over(socket);

    //stompClient.connect({}, function (frame) {

      this.stompClient.send("/app/chat/sendmessagefromsystem-" + email, {},
        JSON.stringify({ 'from': email, 'to': outer.to, 'message': outer.message, 'whatsappMessageType': 'OUTBOUND' }));

      setTimeout(function () {

        outer.message = "";
        outer.getWhatsappMessages(email, outer.to);

      }, 1000)

      outer.updateCheck(outer.to, false);

      let room = '/topic/messages/' + email + '-' + outer.to;

      this.stompClient.subscribe(room, function (messageOutput) {

      });

   // });

  }

  getWhatsappMessages(from: string, to: string) {

    const tokenPayload = atob(localStorage.getItem('token'));
    const username = tokenPayload.split(':')[0];

    let outer = this;

    this.httpClient.get(this.globalService.url + "/get-whatsapp-messages/?username=" + username + "&from=" + from + "&to=" + to).subscribe((response) => {

      this.messages = <Array<any>>response;

      outer.scroll();
      
    });

  }

  getCustomers() {

    const tokenPayload = atob(localStorage.getItem('token'));
    const username = tokenPayload.split(':')[0];


    this.httpClient.get(this.globalService.url + "/get-whatsapp-customers/?username=" + username).subscribe((response) => {

      this.customers = <Array<any>>response;

    });

  }

  initSocket(email: string) {

    this.socket = new SockJS(this.globalService.url + '/chat');
    
    this.stompClient = Stomp.over(this.socket);

    let outer = this;

    this.stompClient.connect({}, function (frame) {

      //outer.logout_();

      let room = '/topic/messages/messageread-' + email;

      outer.stompClient.subscribe(room, function (messageOutput) {
        let json = JSON.parse(messageOutput.body);
        
        outer.updateCheck(json.from, true);

      });


      room = '/topic/messages/qr-' + email;

      outer.stompClient.subscribe(room, function (messageOutput) {

        outer.displayWhatsappIcon = true;

        outer.qRCode = require('qrcode')
        var canvas = document.getElementById('canvas');

        var json = JSON.parse(messageOutput.body);
        
        outer.qRCode.toCanvas(canvas, json.message, function (error) {
          if (error) console.error(error)


        })
        
      });

      room = '/topic/messages/savecustomers-' + email;

      outer.stompClient.subscribe(room, function (messageOutput) {

        outer.getCustomers();

        outer.waLoading = false;

      });

      room = '/topic/messages/ready-' + email;

      outer.stompClient.subscribe(room, function (messageOutput) {

        let json = JSON.parse(messageOutput.body);

        outer.whatsappImageUrl = json.whatsappImageUrl;

        outer.whatsappPushname = json.whatsappPushname;

        outer.displayQRCode = false;

        localStorage.setItem("displayQRCode", <string><unknown>outer.displayQRCode);

        //let contacts = JSON.parse(json.contactsJson.replaceAll("'", "\""));

        //for (var i = 0; i < contacts.length; i++) {

          //console.log(contacts[i]["contact"].number);

        //}

      });

      room = '/topic/messages/sendmessage-' + email;

      outer.stompClient.subscribe(room, function (messageOutput) {

        let json = JSON.parse(messageOutput.body);

        if (json.showWelcomeMessage) {

          outer.sendWelcomeMessage(json.from, json.to);

        };

        if (outer.selectedCustomer != null && outer.selectedCustomer != undefined &&
          outer.selectedCustomer.whatsappNumber == json.to) {

          outer.getWhatsappMessages(email, json.to);

        }

        outer.getCustomers();

      });

      
    });

  }

  initSocketRoom(email: string, to: string) {

    let socket = new SockJS(this.globalService.url + '/chat');

    let stompClient = Stomp.over(socket);

    let outer = this;

    stompClient.connect({}, function (frame) {

      let room = '/topic/messages/' + email + '-' + to;

      stompClient.subscribe(room, function (messageOutput) {

        this.getWhatsappMessages(email, to);    

      });


    });

  }

  onClickCustomer(to: string, customer: Customer) {

    this.globalService.to = to;

    this.newItemEvent.emit(customer);

    this.selectedCustomer = customer;

    const tokenPayload = atob(localStorage.getItem('token'));
    const email = tokenPayload.split(':')[0];

    this.initSocketRoom(email, to);

    this.getZeroMessagesNotViewed(to);

    this.to = to;

    this.getWhatsappMessages(email, this.to);

    this.updateCustomerChecked();

  }

  scroll() {

    let outer = this;

    setTimeout(function () {

      outer.scrollDown();

    }, 500);

  }

  onMouseOverCustomer() {

    if (this.to != null) {

      this.getZeroMessagesNotViewed(this.to);

    }

  }

  getTime(date) {

    if (date == null) {

      return "";

    }

    let today: Date = new Date();
    let _date: Date = new Date(date);

    if (today.getDate() == _date.getDate()) {

      return this.dateService.millisToTime(_date);

    } else if ((today.getDate() - 1) == _date.getDate()) {

      return "Ontem";

    } else {

      return this.dateService.millisToDate(_date);

    }

  }

  getZeroMessagesNotViewed(phone) {

    const tokenPayload = atob(localStorage.getItem('token'));
    const username = tokenPayload.split(':')[0];

    this.httpClient.get(this.globalService.url + "/get-zero-messages-not-viewed/?username=" + username + "&phone=" + phone).subscribe((response) => {

      this.getCustomers();

    });

  }

  getDate(i, millis, millis2) {
    
    let now = this.dateService.millisToDate(Date.now());
    let date = this.dateService.millisToDate(millis);
    let date2 = this.dateService.millisToDate(millis2);

    if (i == 0) {

      if (now == date) {

        return "Hoje";

      } else {

        return date;

      }

    } else if(date != date2){

      if (now == date) {

        return "Hoje";

      } else {

        return date;

      }

    }

  }

  loadCustomers() {

    const tokenPayload = atob(localStorage.getItem('token'));
    const result = tokenPayload.split(':')[0];

    let outer = this;

    //this.stompClient.connect({}, function (frame) {

      outer.waLoading = true;

      outer.stompClient.send("/app/chat/loadcustomers-" + result, {},
        JSON.stringify({ 'from': "", 'to': "", 'message': "", 'whatsappMessageType': "LOAD_CUSTOMERS" }));

    //});

    

  }

  updateCheck(_phone: string, _checked: boolean) {

    let data = JSON.stringify({
      phone: _phone,
      checked: _checked
    })

    const tokenPayload = atob(localStorage.getItem('token'));
    const result = tokenPayload.split(':')[0];

    let outer = this;

    this.httpClient.post(this.globalService.url + "/update-check/?username=" + result, data).subscribe((response) => {

      outer.getCustomers();

    });

  }

  sendCustomMessage(type) {

    Swal.fire({
      title: 'Você tem certeza?',
      text: 'Confirme o envio da mensagem',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sim',
      cancelButtonText: 'Não'
    }).then((result) => {

      if (result.value) {

        const tokenPayload = atob(localStorage.getItem('token'));
        const email = tokenPayload.split(':')[0];

        let outer = this;

        let message;

        this.httpClient.get(this.globalService.url + "/get-whatsapp-custom-messages/?username=" + email).subscribe((response) => {
          
          let messages: WhatsappCustomMessages = <WhatsappCustomMessages>response;
          
          if (messages != null) {

            if (type == "anythingElse") {

              message = JSON.parse(messages.anythingElse);

            } else if (type == "anythingElse") {

              message = JSON.parse(messages.anythingElse);

            } else if (type == "confirmAddress") {

              message = JSON.parse(messages.confirmAddress);

              if (message == null || message == undefined || message.trim() == "") {

                message = "*Confirmar endereço:*\r\n"
                  + ("```Endereço```: *") + (this.selectedCustomer.addresses[0] != undefined ? this.selectedCustomer.addresses[0].address + " " + this.selectedCustomer.addresses[0].number : "") + ("*\r\n")
                  + ("```Complemento: ") + (this.selectedCustomer.addresses[0] != undefined ? this.selectedCustomer.addresses[0].complement : "") + ("```\r\n")
                  + ("```Bairro: ") + (this.selectedCustomer.addresses[0] != undefined ? this.selectedCustomer.addresses[0].district : "") + ("```\r\n")

                outer.send_(message, email);

                return;

              }

            } else if (type == "requestChange") {

              message = JSON.parse(messages.requestChange);

            } else if (type == "sendCustomerOrder") {

              message = JSON.parse(messages.sendCustomerOrder);

              if (message == null || message == undefined || message.trim() == "") {

                let _message = outer.getCustomerOrderMessage();

                outer.send_(<string><unknown>_message, email);

                return;

              }

            } else if (type == "sendMenu") {

              message = JSON.parse(messages.sendMenu);

            } else if (type == "sendOrderTotal") {

              message = JSON.parse(messages.sendOrderTotal);

            } else if (type == "welcome") {

              message = JSON.parse(messages.welcome);

            } else if (type == "outForDelivery") {

              message = JSON.parse(messages.outForDelivery);

            } else if (type == "orderCompleted") {

              message = JSON.parse(messages.orderCompleted);

            }
            
            if (message == null) {

              Swal.fire(
                'Aviso!',
                'A mensagem deve ser registrada em Configurações.',
                'warning'
              )

              return;

            }

            let _messages = message[1];

            outer.send(_messages, email);


          } else {

            Swal.fire(
              'Aviso!',
              'A mensagem deve ser registrada em Configurações.',
              'warning'
            )

          }

        });

      } else if (result.dismiss === Swal.DismissReason.cancel) {

        return;

      }

    })

  }

  send(_messages, email) {
    
    let outer = this;

    for (let i = 0; i < _messages.length; i++) {
     
      let _message = _messages[i];

      if (_message == null || _message == undefined || _message.trim() == "") {

        continue;

      }

      _message = this.getMessageWithVariables(_message);

      setTimeout(function () {

        outer.stompClient.send("/app/chat/sendmessagefromsystem-" + email, {},
          JSON.stringify({ 'from': email, 'to': outer.to, 'message': _message, 'base64Image': _message, 'whatsappMessageType': 'OUTBOUND', 'dateSent': new Date() }));

      }, 500 * i)

      setTimeout(function () {

        outer.getWhatsappMessages(email, outer.to);

      }, 1000)

    }

  }

  send_(_messages: string, email) {
    
    let outer = this;

    let _message: string = <string>_messages;

    _message = this.getMessageWithVariables(_message);

    let names = document.getElementsByClassName("food-name");
    let quantities = document.getElementsByClassName("food-quantity");
    let prices = document.getElementsByClassName("food-price");

    let str = "";

    for (let i = 0; i < names.length; i++) {

      str += ("*" + names.item(i).textContent.trim() + "*") + "\r\n" + (quantities.item(i).textContent) + "             " +
        this.currencyService.formatNumber((<number><unknown>(prices.item(i).textContent.replace("R$", "").trim().split(".").join("").replace(",", ".").replace("&nbsp;", ""))) / 2) +
        "             " + (prices.item(i).textContent)+ "\r\n";

    }

    _message = _message.split("[PRODUTOS]").join(str);

      outer.stompClient.send("/app/chat/sendmessagefromsystem-" + email, {},
      JSON.stringify({ 'from': email, 'to': outer.to, 'message': _message, 'base64Image': _message, 'whatsappMessageType': 'OUTBOUND', 'dateSent': new Date() }));

      setTimeout(function () {

        outer.getWhatsappMessages(email, outer.to);

      }, 1000)

  }

  getMessageWithVariables(_message) {

    _message = _message.split("[R$ 0,00]").join("*R$" + $("#order-total").html() + "*");
    _message = _message.split("[NOME]").join($("#companyName").html());
    _message = _message.split("[CLIENTE]").join(this.selectedCustomer.name);
    _message = _message.split("[ENDERECO]").join(this.selectedCustomer.addresses[0] != undefined ? this.selectedCustomer.addresses[0].address : "");
    _message = _message.split("[NUMERO]").join(this.selectedCustomer.addresses[0] != undefined ? this.selectedCustomer.addresses[0].number : "");
    _message = _message.split("[COMPLEMENTO]").join(this.selectedCustomer.addresses[0] != undefined ? this.selectedCustomer.addresses[0].complement : "");
    _message = _message.split("[BAIRRO]").join(this.selectedCustomer.addresses[0] != undefined ? this.selectedCustomer.addresses[0].district : "");
    _message = _message.split("[TOTAL_PRODUTOS]").join($("#products-total").html());
    _message = _message.split("[TAXA_DE_ENTREGA]").join($("#services-total").html());
    _message = _message.split("[VALOR_TOTAL]").join($("#order-total").html());
    _message = _message.split("[VALOR_A_PAGAR]").join($("#order-total").html());
    _message = _message.split("[TROCO]").join(<string>$("#change").val());

    return _message;

  }

  getCustomerOrderMessage() {

    return "*[NOME]*        \r\n```---------------------------```\r\nSegue o seu pedido, qualquer diverg\u00EAncia, favor informar\r\n```---------------------------```\r\n```Cliente..: [CLIENTE]```\r\n```Endere\u00E7o.:```  *[ENDERECO]* *[NUMERO]*\r\n```Compl....:``` [COMPLEMENTO]\r\n```Bairro...:``` [BAIRRO]\r\n```---------------------------```\r\n```Produto```\r\n```Qtde  Vl. Unit   Vl. Total```\r\n```---------------------------```\r\n [PRODUTOS]\r\n\r\n```Total Produtos.:```  [TOTAL_PRODUTOS]\r\n     ```Taxa Entrega.:```  [TAXA_DE_ENTREGA]\r\n          VALOR TOTAL.:  [VALOR_TOTAL]\r\n``` Valor a Pagar.:```  _[VALOR_A_PAGAR]_\r\n                     ```Troco.:```  _[TROCO]_\r\n\r\nEnviado de forma autom\u00E1tica pelo sistema:\r\nChefsuite\r\n(44) 9 9923-4915\r\nhttp:\/\/bit.ly\/chefsuite";
  }

  sendWelcomeMessage(from: string, to: string) {

  const tokenPayload = atob(localStorage.getItem('token'));
  const email = tokenPayload.split(':')[0];

  let outer = this;

  let message;

    this.httpClient.get(this.globalService.url + "/get-whatsapp-custom-messages/?username=" + email).subscribe((response) => {

      let messages: WhatsappCustomMessages = <WhatsappCustomMessages>response;

      message = JSON.parse(messages.welcome);

      if (message == null) {

        return;

      }

      let _messages = message[1];

      for (let i = 0; i < _messages.length; i++) {

        let _message = _messages[i];

        if (_message == null || _message == undefined || _message.trim() == "") {

          continue;

        }

          this.stompClient.send("/app/chat/sendmessagefromsystem-" + from, {},
            JSON.stringify({ 'from': from, 'to': to, 'message': _message, 'base64Image': _message, 'whatsappMessageType': 'OUTBOUND' }));

          setTimeout(function () {

            if (email == from && outer.to == to) {

              outer.getWhatsappMessages(from, to)

            };

          }, 100)

      }

    });

  }

  logout() {

    const tokenPayload = atob(localStorage.getItem('token'));
    const email = tokenPayload.split(':')[0];

    let outer = this;

    this.stompClient.send("/app/chat/logout-" + email, {},
        JSON.stringify({ 'from': email, 'to': '', 'message': '', 'whatsappMessageType': 'LOGOUT' }));

      setTimeout(function () {

        outer.whatsappImageUrl = null;

        outer.whatsappPushname = null;

        outer.displayQRCode = "";

        localStorage.setItem("displayQRCode", <string><unknown>outer.displayQRCode);

        outer.stompClient.disconnect();

        window.location.reload(false);

      }, 100)

  }

  //used when user has cleared the cache
  logout_() {
    
    const tokenPayload = atob(localStorage.getItem('token'));
    const email = tokenPayload.split(':')[0];
    
    let outer = this;
    
    this.stompClient.send("/app/chat/logout-" + email, {},
      JSON.stringify({ 'from': email, 'to': '', 'message': '', 'whatsappMessageType': 'LOGOUT' }));
    
  }

  ngOnDestroy(): void {

   // console.log("called");

    //const tokenPayload = atob(localStorage.getItem('token'));
    //const email = tokenPayload.split(':')[0];

    let outer = this;

    //this.stompClient.send("/app/chat/logout-" + email, {},
      //JSON.stringify({ 'from': email, 'to': '', 'message': '', 'whatsappMessageType': 'LOGOUT' }));

    //setTimeout(function () {

      //outer.whatsappImageUrl = null;

      //outer.whatsappPushname = null;

      outer.stompClient.disconnect();

    //}, 100)

  }

}
