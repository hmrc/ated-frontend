/*
 * timeout-dialog.js v1.0.1, 01-03-2012
 *
 * @author: Rodrigo Neri (@rigoneri)
 *
 * (The MIT License)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */


/* String formatting, you might want to remove this if you already use it.
 * Example:
 *
 * var location = 'World';
 * alert('Hello {0}'.format(location));
 */

 function secondsToTime(secs)
 {
     var hours = Math.floor(secs / (60 * 60));

     var divisor_for_minutes = secs % (60 * 60);
     var minutes = Math.floor(divisor_for_minutes / 60);

     var divisor_for_seconds = divisor_for_minutes % 60;
     var seconds = Math.ceil(divisor_for_seconds);

     var obj = {
         "h": hours,
         "m": minutes,
         "s": seconds
     };
     return obj;
 }

String.prototype.format = function() {
  var s = this,
      i = arguments.length;

  while (i--) {
    s = s.replace(new RegExp('\\{' + i + '\\}', 'gm'), arguments[i]);
  }
  return s;
};

!function($) {
  $.timeoutDialog = function(options) {

    var settings = {
      timeout: 30,
      countdown: 1,
      time : 'minutes',
      title : 'You’re about to be signed out',
      message : "For your security, you’ll be signed out in <br>{0} {1} if there’s no activity on your account. ",
      keep_alive_button_text: 'Get another {0} minutes',
      keep_alive_url: '/keep-alive',
      logout_url: '/sign-out',
      logout_redirect_url: '/',
      restart_on_yes: true,
      dialog_width: 340,
      close_on_escape: false,
      background_no_scroll: false
    }

    $.extend(settings, options);

    var TimeoutDialog = {
      init: function () {
        this.setupDialogTimer();
      },

      setupDialogTimer: function() {
        var self = this;
        window.setTimeout(function() {
           self.setupDialog();
        }, ((settings.timeout) - (settings.countdown)) * 1000);
      },

      setupDialog: function() {
        dialogOpen = true;
        var self = this;
        self.destroyDialog();
        if(settings.background_no_scroll){$('html').addClass('noScroll');}
        var time =  secondsToTime(settings.countdown)
        var timeout =  secondsToTime(settings.timeout)
         //ignored seconds time.m used below
        $('<div id="timeout-dialog">' +
            '<p id="timeout-message">' + settings.message.format('<span id="timeout-countdown">' + time.m  + '</span>'
            ,'<span id="timeout-Seconds">' + settings.time + '</span>') + '</p>' +
          '</div>')
        .appendTo('body')
        .dialog({
          modal: true,
          width: settings.dialog_width,
          minHeight: 'auto',
          zIndex: 10000,
          title : settings.message.format(time.m,settings.time),
          closeOnEscape: settings.close_on_escape,
          draggable: false,
          resizable: false,
          dialogClass: 'timeout-dialog',
          classes: {
            "ui-dialog": "ui-corner-all",
            "ui-dialog-titlebar": "ui-corner-all visuallyhidden"
          },
          title: settings.title,
          buttons: {
            'keep-alive-button' : {
              text: settings.keep_alive_button_text.format(timeout.m),
              "class": 'button button--link',
              id: "timeout-keep-signin-btn",
              click: function() {
                self.keepAlive();
              }
            }
          }
        });

        self.startCountdown();

        self.escPress = function (event) {
          if (dialogOpen && event.keyCode == 27) {
            // close the dialog
            self.keepAlive();
          }
        }

        document.addEventListener("keydown", self.escPress, true);

      },

      destroyDialog: function() {
        if ($("#timeout-dialog").length) {
          dialogOpen = false;
          $('#timeout-dialog').dialog("close");
          $('#timeout-dialog').remove();
          if(settings.background_no_scroll){$('html').removeClass('noScroll');}
        }
      },

      startCountdown: function() {
        var self = this,
            counter = settings.countdown;

        this.countdown = window.setInterval(function() {
          counter -= 1;
          if (counter <= 60) {
             $("#timeout-countdown").html(counter);
            $("#timeout-Seconds").html('seconds');
          }else if(counter % 60 == 0){
             $("#timeout-countdown").html(counter/60);
             $("#timeout-Seconds").html('minutes');
          }

          if (counter <= 0) {
            window.clearInterval(self.countdown);
            self.signOut(false);
          }

        }, 1000);
      },

      keepAlive: function() {
        var self = this;
        this.destroyDialog();
        window.clearInterval(this.countdown);
        document.removeEventListener("keydown", self.escPress);

        $.get(settings.keep_alive_url, function(data) {
          if (data == "OK") {
            if (settings.restart_on_yes) {
              self.setupDialogTimer();
            }
          }
          else {
            self.signOut(false);
          }
        });
      },

      signOut: function(is_forced) {
        var self = this;
        this.destroyDialog();

        if (settings.logout_url != null) {
            window.location = settings.logout_url;
        }
        else {
            self.redirectLogout(is_forced);
        }
      },

      redirectLogout: function(is_forced){
        var target = settings.logout_redirect_url;
        if (!is_forced)
        window.location = target;
      }
    };

    TimeoutDialog.init();
  };
}(window.jQuery);
