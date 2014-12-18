"use strict"
var ADManager = function(){
    var self = this;
    if(ADManager.instance!=undefined){
        return ADManager.instance;
    }
    ADManager.unique = self;
    // video ad list
    self.adList = null;
    self.adMap = null;
    self.currAdTime = null;
    /**Simple XMLHTTPRequest**/
    self.simpleHttpRequest = function(method, headers, url, data, callback){
        console.info("url: ", url);
        var xhr = new XMLHttpRequest({mozSystem: true});
        xhr.open(method, url, true);
        if(headers){
            for (var i = headers.length - 1; i >= 0; i--) {
                xhr.setRequestHeader(headers[i][0], headers[i][1]);
            };
        }
        xhr.onreadystatechange = function() {
            if(xhr.readyState==4){
                if(xhr.status==200||xhr.status==201){
                    if(callback){
                        callback(xhr.responseText);
                    }
                }else{
                    console.info("XmlHttpRequest Error", xhr.readyState, xhr.status);
                }
            }
        }
        xhr.onerror = function() {
            console.info('error'); 
        };
        xhr.timeout = 2000;
        if(data){
            xhr.send(JSON.stringify(data));
        }else{
            xhr.send();
        }
    };
    /**
    * ad manager loadding
    * @param {Object} MediaPlayer Instance
    */
    self.load = function(player){
        var adMessageBus = player.receiverWrapper.createMessageBus("urn:flint:org.openflint.fling.media_ad");
        player.video.addEventListener("loadedmetadata", function (e) {
            self.adList = null;
            self.adMap = null;
            self.currAdTime = null;
            self.simpleHttpRequest("get", null, player.videoURL+".ad", null, function(resp){
                self.adList = JSON.parse(resp);
                if(self.adList.length){
                    self.adMap = {};
                    for(var i=0;i<self.adList.length;i++){
                        self.adMap[self.adList[i].time] = self.adList[i];
                    }
                }
            });
        });
        player.video.addEventListener("timeupdate", function (e) {
            var sec = Math.floor(player.video.currentTime);
            if(typeof(self.adMap[sec])!="undefined"&& self.currAdTime!=sec){
                self.currAdTime = sec;
                
                console.info(self.adMap[sec]);
                var adData = {
                    "time": self.adMap[sec].time,
                    "video_url": player.videoURL,
                    "video_title": player.title,
                    "type": self.adMap[sec].type,
                    "ad_data": self.adMap[sec].ad_data
                }
                var adJson = JSON.stringify(adData);
                adMessageBus.send(adJson, player.senderId);
            }
        });
    };
    return ADManager.unique;
};