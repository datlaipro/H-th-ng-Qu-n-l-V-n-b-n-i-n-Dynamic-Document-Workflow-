import {
  MAT_SELECT_CONFIG,
  MAT_SELECT_SCROLL_STRATEGY,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER_FACTORY,
  MAT_SELECT_TRIGGER,
  MatSelect,
  MatSelectChange,
  MatSelectModule,
  MatSelectTrigger
} from "./chunk-REEUBHDC.js";
import "./chunk-B5BTB5DG.js";
import {
  MatError,
  MatFormField,
  MatHint,
  MatLabel,
  MatPrefix,
  MatSuffix
} from "./chunk-LOR7VRW6.js";
import "./chunk-AHVWMHDC.js";
import {
  MatOptgroup,
  MatOption
} from "./chunk-ASHIY5EN.js";
import "./chunk-BRFLWEMC.js";
import "./chunk-PYEXC7SF.js";
import "./chunk-NYOW4HPM.js";
import "./chunk-WZGGNXIW.js";
import "./chunk-XJKF6JJY.js";
import "./chunk-V53BAHM2.js";
import "./chunk-WLMPYXW6.js";
import "./chunk-HCQDUCWN.js";
import "./chunk-CMS3GY4Q.js";
import "./chunk-7A5ZFYCB.js";
import "./chunk-GWFLKVBH.js";
import "./chunk-UTB2XLNV.js";
import "./chunk-YQTMVYBE.js";
import "./chunk-2WBYOXEK.js";
import "./chunk-5JQHIHNF.js";
import "./chunk-7UJZXIJQ.js";
import "./chunk-RMFGBTI6.js";
import "./chunk-B6Q77AK3.js";
import "./chunk-J2CJJ44I.js";
import "./chunk-QCDBD2C7.js";
import "./chunk-4X6VR2I6.js";
import "./chunk-SYQEHEBD.js";
import "./chunk-5YKDIGWC.js";
import "./chunk-JRFR6BLO.js";
import "./chunk-HWYXSU2G.js";
import "./chunk-MARUHEWW.js";
import "./chunk-WDMUDEB6.js";

// node_modules/@angular/material/fesm2022/select.mjs
var matSelectAnimations = {
  // Represents
  // trigger('transformPanel', [
  //   state(
  //     'void',
  //     style({
  //       opacity: 0,
  //       transform: 'scale(1, 0.8)',
  //     }),
  //   ),
  //   transition(
  //     'void => showing',
  //     animate(
  //       '120ms cubic-bezier(0, 0, 0.2, 1)',
  //       style({
  //         opacity: 1,
  //         transform: 'scale(1, 1)',
  //       }),
  //     ),
  //   ),
  //   transition('* => void', animate('100ms linear', style({opacity: 0}))),
  // ])
  /** This animation transforms the select's overlay panel on and off the page. */
  transformPanel: {
    type: 7,
    name: "transformPanel",
    definitions: [
      {
        type: 0,
        name: "void",
        styles: {
          type: 6,
          styles: { opacity: 0, transform: "scale(1, 0.8)" },
          offset: null
        }
      },
      {
        type: 1,
        expr: "void => showing",
        animation: {
          type: 4,
          styles: {
            type: 6,
            styles: { opacity: 1, transform: "scale(1, 1)" },
            offset: null
          },
          timings: "120ms cubic-bezier(0, 0, 0.2, 1)"
        },
        options: null
      },
      {
        type: 1,
        expr: "* => void",
        animation: {
          type: 4,
          styles: { type: 6, styles: { opacity: 0 }, offset: null },
          timings: "100ms linear"
        },
        options: null
      }
    ],
    options: {}
  }
};
export {
  MAT_SELECT_CONFIG,
  MAT_SELECT_SCROLL_STRATEGY,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER_FACTORY,
  MAT_SELECT_TRIGGER,
  MatError,
  MatFormField,
  MatHint,
  MatLabel,
  MatOptgroup,
  MatOption,
  MatPrefix,
  MatSelect,
  MatSelectChange,
  MatSelectModule,
  MatSelectTrigger,
  MatSuffix,
  matSelectAnimations
};
//# sourceMappingURL=@angular_material_select.js.map
