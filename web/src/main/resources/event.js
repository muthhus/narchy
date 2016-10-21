"use strict";

var DEFAULT_MAX_LISTENERS = 12;

//TODO use ES6 Map for better performance: http://jsperf.com/map-vs-object-as-hashes/2
class EventEmitter {
    constructor(){
        this._maxListeners = DEFAULT_MAX_LISTENERS;
        this._events = {} //TODO use ES6 Map
    }
    on(type, listener) {

        var that = this;
        if (Array.isArray(type)) {
            _.each(type, function(t) {
                that.on(t, listener);
            });
            return;
        }

        if(typeof listener != "function") {
            throw new TypeError()
        }
        var listeners = this._events[type] ||(this._events[type] = []);
        if(listeners.indexOf(listener) != -1) {
            return this
        }
        listeners.push(listener);
        if(listeners.length > this._maxListeners) {
            error(
                "possible memory leak, added %i %s listeners, "+
                "use EventEmitter#setMaxListeners(number) if you " +
                "want to increase the limit (%i now)",
                listeners.length,
                type,
                this._maxListeners
            )
        }
        return this
    }
    once(type, listener) {
        var eventsInstance = this;
        function onceCallback(){
            eventsInstance.off(type, onceCallback);
            listener.apply(null, arguments)
        }
        return this.on(type, onceCallback)
    }
    off(type, listener) {

        var that = this;

        if (type === undefined) {
            //disable everythign
            for (var k in this._events) {
                this.off(k);
            }
            return;
        } else if (Array.isArray(type)) {
            _.each(type, function(t) {
                that.off(t, listener);
            });
            return;
        }

        var listeners = this._events[type];

        if (listener === undefined) {
            //remove any existing
            if (listeners) {
                this.off(type, listeners);
            }
            return;

        } else if (Array.isArray(listener)) {
            _.each(listener, function(l) {
                that.off(type, l);
            });
        } else if(typeof listener != "function") {
            throw new TypeError()
        }

        if(!listeners || !listeners.length) {
            return this
        }
        var indexOfListener = listeners.indexOf(listener);
        if(indexOfListener == -1) {
            return this
        }
        listeners.splice(indexOfListener, 1);
        return this
    }
    emit(type, args){
        const listeners = this._events[type];
        if(!listeners || !listeners.length) {
            return false
        }
        setTimeout( ()=> {
            for (var i = 0; i < listeners.length; i++)
                listeners[i].apply(null, args);
        }, 0);
        //listeners.forEach(function(fn) { fn.apply(null, args) })
        return true
    }
    setMaxListeners(newMaxListeners){
        if(parseInt(newMaxListeners) !== newMaxListeners) {
            throw new TypeError()
        }
        this._maxListeners = newMaxListeners
    }
}

