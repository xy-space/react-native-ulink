import { NativeModules, NativeEventEmitter } from 'react-native';

const { Ulink } = NativeModules;
const listenerCache = {};

export interface EVENT_TYPE {
    onLink: string;
}
const handlerLink = () => {
    const eventEmitter = new NativeEventEmitter(Ulink);
    Ulink.handlerLink();

    return {
        subscribe: (type: keyof EVENT_TYPE, callback: (event: any) => void) => {
            if (listenerCache[type]) {
                listenerCache[type].remove();
            }
            return (listenerCache[type] = eventEmitter.addListener(type, (event: any) => {
                console.log(`onLink event`, event);
                callback(event);
            }));
        },
    };
};

export default {
    handlerLink,
    init: Ulink.init,
};
