"use strict";
const DEFAULT_MAX_LISTENERS = 12;
//TODO use ES6 Map for better performance: http://jsperf.com/map-vs-object-as-hashes/2
class EventEmitter {
    constructor() {
        this._maxListeners = DEFAULT_MAX_LISTENERS;
        this._events = new Map();
    }
    on(type, listener) {
        const that = this;
        if (Array.isArray(type)) {
            for (let t of type)
                that.on(t, listener);
            return this;
        }
        if (typeof listener != "function") {
            throw new TypeError();
        }
        let listeners = this._events.get(type);
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
    }
    once(type, listener) {
        const eventsInstance = this;
        function onceCallback() {
            eventsInstance.off(type, onceCallback);
            listener.apply(null, arguments);
        }
        return this.on(type, onceCallback);
    }
    off(type, listener = null) {
        const that = this;
        if (type === undefined) {
            //disable everythign
            throw ('unimpl yet');
        }
        else if (Array.isArray(type)) {
            for (let tt of type) {
                that.off(tt, listener);
            }
            return;
        }
        let listeners = this._events.get(type);
        if (listener === undefined) {
            //remove any existing
            if (listeners) {
                this.off(type, listeners);
            }
            return;
        }
        else if (Array.isArray(listener)) {
            for (let l of type) {
                that.off(type, l);
            }
        }
        else if (typeof listener != "function") {
            throw new TypeError();
        }
        if (!listeners || !listeners.length) {
            return;
        }
        const indexOfListener = listeners.indexOf(listener);
        if (indexOfListener == -1) {
            return;
        }
        listeners.splice(indexOfListener, 1);
        if (listeners.length === 0) {
            this._events.delete(type); //clear its entry
        }
    }
    emit(type, ...args) {
        const listeners = this._events.get(type);
        if (listeners) {
            for (let x of listeners) {
                x.apply(null, args);
            }
        }
        //for (let i = 0; i < listeners.length; i++)
        //listeners.forEach(function(fn) { fn.apply(null, args) })
        return true;
    }
    setMaxListeners(newMaxListeners) {
        if (parseInt(newMaxListeners) !== newMaxListeners) {
            throw new TypeError();
        }
        this._maxListeners = newMaxListeners;
    }
}
//# sourceMappingURL=EventEmitter.js.map