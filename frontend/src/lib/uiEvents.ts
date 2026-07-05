import type { ToastTone } from '../components/ToastStack';

export const BLUEBERRY_TOAST_EVENT = 'blueberrytrace:toast';

export interface BlueberryToastDetail {
  tone: ToastTone;
  title: string;
  description?: string;
}

export function emitToast(tone: ToastTone, title: string, description?: string) {
  window.dispatchEvent(new CustomEvent<BlueberryToastDetail>(BLUEBERRY_TOAST_EVENT, {
    detail: { tone, title, description }
  }));
}
