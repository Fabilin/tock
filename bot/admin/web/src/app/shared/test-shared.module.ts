import { APP_BASE_HREF, CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';
import {
  NbButtonModule,
  NbCardModule,
  NbCheckboxModule,
  NbDatepickerModule,
  NbDialogModule,
  NbFormFieldModule,
  NbIconLibraries,
  NbIconModule,
  NbInputModule,
  NbRadioModule,
  NbSelectModule,
  NbSpinnerModule,
  NbThemeModule,
  NbToastrModule,
  NbTooltipModule,
  NbWindowModule
} from '@nebular/theme';
import { TranslocoTestingModule, TranslocoTestingOptions } from '@jsverse/transloco';
import { DialogService } from '../core-nlp/dialog.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RestService } from '../core-nlp/rest/rest.service';
import { BotService } from '../bot/bot-service';

export function getTranslocoTestingModule(options: TranslocoTestingOptions = {}) {
  return TranslocoTestingModule.forRoot({
    langs: { en: {}, fr: {} },
    translocoConfig: { availableLangs: ['en', 'fr'], defaultLang: 'en' },
    preloadLangs: true,
    ...options
  });
}

@NgModule({
  imports: [
    BrowserAnimationsModule,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forRoot([]),
    NbThemeModule.forRoot({ name: 'default' }),
    getTranslocoTestingModule(),
    NbCardModule,
    NbButtonModule,
    NbIconModule,
    NbInputModule,
    NbSelectModule,
    NbFormFieldModule,
    NbCheckboxModule,
    NbTooltipModule,
    NbSpinnerModule,
    NbToastrModule.forRoot(),
    NbWindowModule.forRoot(),
    NbDialogModule.forRoot(),
    HttpClientTestingModule,
    NbDatepickerModule.forRoot(),
    NbRadioModule
  ],
  exports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TranslocoTestingModule,
    NbCardModule,
    NbButtonModule,
    NbIconModule,
    NbInputModule,
    NbSelectModule,
    NbFormFieldModule,
    NbCheckboxModule,
    NbTooltipModule,
    NbSpinnerModule,
    NbToastrModule,
    NbWindowModule,
    NbDialogModule,
    NbDatepickerModule,
    NbRadioModule
  ],
  providers: [DialogService, RestService, BotService, { provide: APP_BASE_HREF, useValue: '/' }]
})
export class TestSharedModule {
  constructor(private iconLibraries: NbIconLibraries) {
    this.iconLibraries.registerFontPack('bootstrap-icons', { iconClassPrefix: 'bi' });
    this.iconLibraries.setDefaultPack('bootstrap-icons');
  }
}
