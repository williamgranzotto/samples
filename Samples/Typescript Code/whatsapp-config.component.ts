import { Component, OnInit } from '@angular/core';
import { URLService } from '../services/url.service';
import { faEllipsisH } from '@fortawesome/free-solid-svg-icons';
import { faCommentDots } from '@fortawesome/free-solid-svg-icons';
import { faCamera } from '@fortawesome/free-solid-svg-icons';
import { faTrash } from '@fortawesome/free-solid-svg-icons';
import { faGrinAlt } from '@fortawesome/free-solid-svg-icons';
import { TabService } from '../services/tab.service';
import { GlobalService } from '../services/global.service';
import { HttpClient } from '@angular/common/http';
import Swal from 'sweetalert2';
import { WhatsappCustomMessages } from '../dto/WhatsappCustomMessages';
import { EmojiButton } from '@joeattardi/emoji-button';
import { FormGroup, FormBuilder, FormControl } from '@angular/forms';
import { BrMaskDirective, BrMaskModel } from 'br-mask';
import { FormService } from '../services/form.service';
import { WhatsappConfig } from '../dto/WhatsappConfig';
import { CurrencyService } from '../services/currency.service';

@Component({
  selector: 'app-whatsapp-config',
  templateUrl: './whatsapp-config.component.html',
  styleUrls: ['./whatsapp-config.component.css']
})
export class WhatsappConfigComponent implements OnInit {

  public tab: string;

  faEllipsisH = faEllipsisH;
  faCommentDots = faCommentDots;
  faCamera = faCamera;
  faTrash = faTrash;
  faGrinAlt = faGrinAlt;

  map = new Map<string, Array<string>>();

  content = new Map<string, Array<string>>();

  files = new Array<File>();

  tabname = "welcome";

  tabname_ = "wa-messages";

  picker;
  trigger: HTMLElement;

  waConfigForm: FormGroup;

  constructor(public urlService: URLService, public tabService: TabService, public globalService: GlobalService,
    public httpClient: HttpClient, public formBuilder: FormBuilder, public formService: FormService,
    public currencyService: CurrencyService) { }

  ngOnInit(): void {

    this.tab = this.urlService.getParameter(window.location.href, "tab");

    this.initArr("welcome");
    this.initArr("anythingElse");
    this.initArr("confirmAddress");
    this.initArr("requestChange");
    this.initArr("sendCustomerOrder");
    this.initArr("sendMenu");
    this.initArr("sendOrderTotal");
    this.initArr("outForDelivery");
    this.initArr("orderCompleted");

    const tokenPayload = atob(localStorage.getItem('token'));
    let username = tokenPayload.split(':')[0];

    this.httpClient.get(this.globalService.url + "/get-whatsapp-custom-messages/?username=" + username).subscribe((response) => {

      let messages: WhatsappCustomMessages = <WhatsappCustomMessages>response;

      if (messages != null) {

        let anythingElse = JSON.parse(messages.anythingElse);
        this.pushArray("anythingElse", anythingElse);

        let confirmAddress = JSON.parse(messages.confirmAddress);
        this.pushArray("confirmAddress", confirmAddress);

        let requestChange = JSON.parse(messages.requestChange);
        this.pushArray("requestChange", requestChange);

        let sendCustomerOrder = JSON.parse(messages.sendCustomerOrder);
        this.pushArray("sendCustomerOrder", sendCustomerOrder);

        let sendMenu = JSON.parse(messages.sendMenu);
        this.pushArray("sendMenu", sendMenu);

        let sendOrderTotal = JSON.parse(messages.sendOrderTotal);
        this.pushArray("sendOrderTotal", sendOrderTotal);

        let welcome = JSON.parse(messages.welcome);
        this.pushArray("welcome", welcome);

        let outForDelivery = JSON.parse(messages.outForDelivery);
        this.pushArray("outForDelivery", outForDelivery);

        let orderCompleted = JSON.parse(messages.orderCompleted);
        this.pushArray("orderCompleted", orderCompleted);

      }

    });

    this.createForms();

    let outer = this;

    this.httpClient.get(this.globalService.url + "/get-wa-config/?username=" + username).subscribe((response) => {

      let waConfig: WhatsappConfig = <WhatsappConfig>response;

      if (waConfig != null) {

        outer.waConfigForm.get("servicesPercentage").setValue(outer.currencyService.formatNumber(waConfig.servicesPercentage));
        outer.waConfigForm.get("discountsPercentage").setValue(outer.currencyService.formatNumber(waConfig.discountsPercentage));
        outer.waConfigForm.get("servicesMoney").setValue(outer.currencyService.formatNumber(waConfig.servicesMoney));
        outer.waConfigForm.get("discountsMoney").setValue(outer.currencyService.formatNumber(waConfig.discountsMoney));
        outer.waConfigForm.get("deliveryTime").setValue(waConfig.deliveryTime);

      } else {

        outer.waConfigForm.get("servicesPercentage").setValue(outer.currencyService.formatNumber(0));
        outer.waConfigForm.get("discountsPercentage").setValue(outer.currencyService.formatNumber(0));
        outer.waConfigForm.get("servicesMoney").setValue(outer.currencyService.formatNumber(0));
        outer.waConfigForm.get("discountsMoney").setValue(outer.currencyService.formatNumber(0));
        outer.waConfigForm.get("deliveryTime").setValue(45);

      }

    });

  }

  createForms() {

    this.waConfigForm = this.formBuilder.group({
      servicesPercentage: new FormControl(''),
      discountsPercentage: new FormControl(''),
      servicesMoney: new FormControl(''),
      discountsMoney: new FormControl(''),
      deliveryTime: new FormControl('')
    });

  }

  initArr(str) {

    this.map.set(str, new Array<string>());

    this.content.set(str, new Array<string>());

  }

  pushArray(str, arr) {

    if (arr != null) {

      for (let i = 0; i < arr[0].length; i++) {

        this.map.get(str).push(arr[0][i]);

      }

      for (let i = 0; i < arr[0].length; i++) {

        this.content.get(str).push(arr[1][i]);

      }

    }

  }

  ngAfterViewInit(): void {

    this.openTab(null, "welcome");

    let outer = this;

    setTimeout(function () {

      outer.picker = new EmojiButton();
      
      outer.trigger = document.querySelector('#emoji-trigger');
      
      outer.picker.on('emoji', selection => {
        // handle the selected emoji here
        //this.message += selection.emoji;

        outer.content.get(outer.tabname)[outer.content.get(outer.tabname).length - 1] += selection.emoji;

      });

      outer.trigger.addEventListener('click', () => {

        outer.picker.togglePicker(outer.trigger)

      });

    }, 1000);

  }

  changeZIndex() {

    let emojis = document.getElementsByClassName("emoji-picker__wrapper");
    emojis.item(0)["style"]["z-index"] = 9999;

  }

  openTab(evt, tabName) {

    this.tabService.openTab(evt, tabName);

    this.tabname = tabName;

  }

  openTab_(evt, tabName) {

    this.tabService.openTab_(evt, tabName);

    this.tabname_ = tabName;

    

  }

  addWhatsappConversation(tab: string, type: string) {

    this.map.get(tab).push(type);

    this.content.get(tab).push("");

  }

  removeWhatsappConversation(tab: string, i: number) {

    this.map.get(tab).pop();

    this.content.get(tab).pop();

  }

  fileChange(event, i) {
    
    let fileList: FileList = event.target.files;

    let file: File;

    if (fileList.length > 0) {

      let _file: File = fileList[0];
      file = _file;

    }

    const toBase64 = file => new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result);
      reader.onerror = error => reject(error);
    });

    let outer = this;

    async function Main() {
      
      const result: unknown = await toBase64(file).catch(e => Error(e));

      outer.content.get(outer.tabname).splice(i, 0, <string><unknown>result)

      if (result instanceof Error) {
        console.log('Error: ', result.message);
        return;
      }
      //...
    }

    Main();

  }

  saveMessage(_tab: string) {

    let arr = new Array<Array<string>>();

    arr.push(this.map.get(_tab));
    arr.push(this.content.get(_tab));

    let data = JSON.stringify({
      tab: _tab,
      content: JSON.stringify(arr)
    })

    const tokenPayload = atob(localStorage.getItem('token'));
    let username = tokenPayload.split(':')[0];

    this.httpClient.post(this.globalService.url + "/save-whatsapp-custom-message/?username=" + username, data).subscribe((response) => {

        if (response["response"] == "success") {

          Swal.fire(
            'Pronto!',
            'Operação realizada com sucesso!',
            'success'
          )

        } else if (response["response"] == "error") {

          Swal.fire(
            'Oops!',
            'Erro ao tentar realizar operação! Tente novamente daqui a alguns minutos.',
            'error'
          )

        }

    });

  }

  saveConfig() {

    if (this.waConfigForm.valid) {

      const tokenPayload = atob(localStorage.getItem('token'));
      const username = tokenPayload.split(':')[0];

      let data = JSON.stringify({
        servicesPercentage: this.waConfigForm.value["servicesPercentage"].split(".").join("").replace(",", "."),
        discountsPercentage: this.waConfigForm.value["discountsPercentage"].split(".").join("").replace(",", "."),
        servicesMoney: this.waConfigForm.value["servicesMoney"].split(".").join("").replace(",", "."),
        discountsMoney: this.waConfigForm.value["discountsMoney"].split(".").join("").replace(",", "."),
        deliveryTime: this.waConfigForm.value["deliveryTime"]
      })

      let outer = this;

      this.httpClient.post(this.globalService.url + "/save-wa-config/?username=" + username, data).subscribe((response) => {

        let _response: string = <string>response;

        outer.formService.hideRequiredMessages();
        outer.formService.showRequiredMessages();

        if (_response["response"] == "success") {

          Swal.fire(
            'Pronto!',
            'Operação realizada com sucesso!',
            'success'
          )

        }

      });

    } else {

      this.formService.hideRequiredMessages();
      this.formService.showRequiredMessages();

    }

  }

}
