"use strict";
var DEFAULT_MAX_LISTENERS = 12;
//TODO use ES6 Map for better performance: http://jsperf.com/map-vs-object-as-hashes/2
var EventEmitter = (function () {
    function EventEmitter() {
        this._maxListeners = DEFAULT_MAX_LISTENERS;
        this._events = new Map();
    }
    EventEmitter.prototype.on = function (type, listener) {
        var that = this;
        if (Array.isArray(type)) {
            for (var _i = 0, type_1 = type; _i < type_1.length; _i++) {
                var t = type_1[_i];
                that.on(t, listener);
            }
            return this;
        }
        if (typeof listener != "function") {
            throw new TypeError();
        }
        var listeners = this._events.get(type);
        if (!listeners) {
            listeners = [listener];
            this._events.set(type, listeners);
        }
        else if (listeners.indexOf(listener) != -1) {
            console.error('duplicate add:', type, listener);
            return this;
        }
        else {
            listeners.push(listener);
            if (listeners.length > this._maxListeners) {
                console.error("possible memory leak, added %i %s listeners, " +
                    "use EventEmitter#setMaxListeners(number) if you " +
                    "want to increase the limit (%i now)", listeners.length, type, this._maxListeners);
            }
        }
        return this;
    };
    EventEmitter.prototype.once = function (type, listener) {
        var eventsInstance = this;
        function onceCallback() {
            eventsInstance.off(type, onceCallback);
            listener.apply(null, arguments);
        }
        return this.on(type, onceCallback);
    };
    EventEmitter.prototype.off = function (type, listener) {
        if (listener === void 0) { listener = null; }
        var that = this;
        if (type === undefined) {
            //disable everythign
            throw ('unimpl yet');
        }
        else if (Array.isArray(type)) {
            for (var _i = 0, type_2 = type; _i < type_2.length; _i++) {
                var tt = type_2[_i];
                that.off(tt, listener);
            }
            return;
        }
        var listeners = this._events.get(type);
        if (listener === undefined) {
            //remove any existing
            if (listeners) {
                this.off(type, listeners);
            }
            return;
        }
        else if (Array.isArray(listener)) {
            for (var _a = 0, type_3 = type; _a < type_3.length; _a++) {
                var l = type_3[_a];
                that.off(type, l);
            }
        }
        else if (typeof listener != "function") {
            throw new TypeError();
        }
        if (!listeners || !listeners.length) {
            return;
        }
        var indexOfListener = listeners.indexOf(listener);
        if (indexOfListener == -1) {
            return;
        }
        listeners.splice(indexOfListener, 1);
        if (listeners.length === 0) {
            this._events.delete(type); //clear its entry
        }
    };
    EventEmitter.prototype.emit = function (type, args) {
        var listeners = this._events.get(type);
        if (listeners) {
            for (var _i = 0, listeners_1 = listeners; _i < listeners_1.length; _i++) {
                var x = listeners_1[_i];
                x.apply(null, args);
            }
        }
        //for (let i = 0; i < listeners.length; i++)
        //listeners.forEach(function(fn) { fn.apply(null, args) })
        return true;
    };
    EventEmitter.prototype.setMaxListeners = function (newMaxListeners) {
        if (parseInt(newMaxListeners) !== newMaxListeners) {
            throw new TypeError();
        }
        this._maxListeners = newMaxListeners;
    };
    return EventEmitter;
}());
//# sourceMappingURL=EventEmitter.js.map