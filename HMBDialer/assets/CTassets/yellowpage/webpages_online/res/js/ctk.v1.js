var ctk_exist=true;var CTK={handler:window.CTKJavaScriptHandler,getApiLevel:function(){if(this.handler){return this.handler.getApiLevel()}else{return 0}},getItemFromStorage:function(a){if(this.handler){return this.handler.storageGetItem(a)}else{return localStorage.getItem(a)}},setItemToStorage:function(a,b){if(this.handler){if(this.handler.getApiLevel()>=30){if(typeof(b)=="number"||typeof(b)=="boolean"){b=""+b}}this.handler.storageSetItem(a,b)}else{localStorage.setItem(a,b)}},removeItemFromStorage:function(a){if(this.handler){this.handler.storageRemoveItem(a)}else{localStorage.removeItem(a)}},appendScenarioNode:function(a){if(this.handler){this.handler.appendScenarioNode(a)}},onUMengEvent:function(a){if(this.handler){if(this.getApiLevel()>=2){return this.handler.onUMengEvent(a)}}},onUMengRecodeUsage:function(c,a,b){if(this.handler){if(this.getApiLevel()>=2){return this.handler.recordUsage(c,a,b)}}},makePhoneCall:function(d,b,a){if(this.handler){Utils.collect_scenario_action_phone_call_params(d);var c=this.getApiLevel();if(c>=4){this.handler.callPhoneLevel4(a,b?b:"",false)}else{if(c>=2){this.handler.callPhoneLevel2(a,b?b:"")}else{this.handler.callPhone(a)}}}else{window.location.href="tel:"+a}},saveCoupon:function(a){if(this.handler){Utils.coupon_info=a;this.handler.saveImage(Utils.coupon_info.img_url,"Utils.save_image_cb")}},openMapAddress:function(b,e,d,a){if(this.handler){this.handler.openMap(b)}else{if(!e){e="全国"}if(a){var c=e+b;window.location.href="http://maps.apple.com/maps?q="+c}else{window.location.href="bmap.html?city="+e+"&address="+b+"&shop="+d}}},openMapLoc:function(b,a){if(this.handler){this.handler.openMapGeo(b,a)}},showToast:function(b){if(isIOS){var a=document.createElement("p");a.style.cssText="position: fixed; top: 40%; left: 50%; z-index: 10000; width: 24.8rem; height: 4.2rem; line-height: 4.2rem; margin-left: -12.4rem; text-align: center; border-radius: 0.3rem; color: #fff; font-size: 1.5rem; background-color: rgba(0, 0, 0, 0.75); -webkit-transition: all linear 1s; opacity: 1";a.innerHTML=b;document.body.appendChild(a);setTimeout(function(){a.style.opacity=0;setTimeout(function(){document.body.removeChild(a)},1200)},1000);return}if(this.getApiLevel()>=2){return this.handler.showToast(b)}else{alert(b)}},redirectLink:function(a,c,b){if(this.handler){if(c in Utils.source_umeng_map){this.onUMengEvent(Utils.source_umeng_map[c])}if(b=="picture"){window.location=a}else{Utils.collect_scenario_status_external_link_params(a);this.handler.startExternalLinkWebView(a,c)}}else{window.location=a}},getTabHeight:function(){if(this.handler){return this.handler.getTabbarHeightInPixels()}else{return 0}},getToken:function(){if(this.handler){return this.handler.getAuthToken()}else{return""}},setToken:function(){if(this.handler){var c=Utils.netService;var b=this.handler.getAuthToken();var a=Utils.getCookie("auth_token");if(a&&a!==b){Utils.removeCookie("auth_token",{domain:c})}Utils.setCookie("auth_token",{domain:c})}},isNetworkAvailable:function(){if(Utils.getApiLevel()>=2){return this.handler.isNetworkAvailable()}else{return true}},getDeviceInfo:function(){if(this.handler){return this.handler.getActivationJsonInfo()}},goBack:function(){if(this.handler){this.handler.backPage()}else{history.back()}},getCellInfo:function(){if(this.handler){return this.handler.getCellInfo()}},locate:function(){var a={latitude:-1,longitude:-1};var b=function(e){var f=new Array();f.push(e.latitude);f.push(e.longitude);console.log("got html position: "+JSON.stringify(f));CTK.setItemToStorage("native_param_location",JSON.stringify(f))};var d=function(e){var f=e.coords;if(f){b(f)}};var c=function(e){b(a)};if(this.handler){return this.handler.locate()}else{b(a);if(navigator.geolocation){navigator.geolocation.getCurrentPosition(d,c,{enableHighAccuracy:true})}}},isLocationServiceAvailable:function(){if(this.handler){if(this.getApiLevel()>=2){return this.handler.isLocationSerivceAvailable()}}return true},isDualSimPhone:function(){if(this.getApiLevel()>=8&&this.handler){var b=this.handler.getReadySim();var a=this.handler.getDefaultSimCard();return((b==3&&a==0)||(b==0&&a==0))}return false},getNetName:function(){if(this.handler){return this.handler.getNetName()}else{return null}},hasTabMark:function(b){if(this.getApiLevel()>=7&&this.handler){var a=this.handler.hasTabMark(b);console.log(a);return a}},removeTabMark:function(a){if(this.getApiLevel()>=7&&this.handler){console.log(this.handler.hasTabMark(a));this.handler.removeTabMark(a);console.log(this.handler.hasTabMark(a))}},setChoosedCity:function(a){if(this.getApiLevel()>=7&&this.handler){this.handler.setChooseCity(a)}},shareInfo:function(b){if(this.getApiLevel()>=7&&this.handler){if(b.type=="sms"){var a=b.msg+" "+b.url;this.handler.sendMessage("sms","",a)}else{if(b.type=="copy"){var a=b.msg+" "+b.url;this.handler.copyToClipboard(a);Utils.showToast("链接复制成功")}else{this.handler.shareMessage(JSON.stringify(b))}}}},scanQRCode:function(){if(this.getApiLevel()>=9&&this.handler){this.handler.scanQRCode()}},showRightTopMenu:function(){if(this.getApiLevel()>=9&&this.handler){var c=window.location.pathname;c=c.substring(c.lastIndexOf("/")+1);var d=Utils.right_top_menu_map[c];var a=[];if(d&&d.length>0){for(var b=0;b<d.length;++b){a.push(Utils.native_all_right_top_menu[d[b]])}}this.handler.setRightTopMenu(JSON.stringify(a),false);if(c=="index.html"){d=Utils.right_top_menu_map["default"];a=[];if(d&&d.length>0){for(var b=0;b<d.length;++b){a.push(Utils.native_all_right_top_menu[d[b]])}}this.handler.setRightTopMenu(JSON.stringify(a),true)}}},isInstalled:function(a){if(this.getApiLevel()>=8){return this.handler.isInstalled(a,"")}return false},alipay:function(a,b){if(this.handler){return this.handler.alipay(a,b)}},isLogged:function(){if(this.handler){return this.handler.isLogged()}},login:function(b,a,c){if(this.handler){return this.handler.login(b,a,c)}},getLoginNumber:function(){if(this.handler){return this.handler.getLoginNumber()}},getPhoneNumber:function(){if(this.handler){return this.handler.getPhoneNumber()}},sendMessage:function(a,c,b){if(this.handler){return this.handler.sendMessage(a,c,b)}},getSecret:function(){if(this.handler){return this.handler.getSecret()}}};