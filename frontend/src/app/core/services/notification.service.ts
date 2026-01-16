import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface Notification {
  id: number;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  duration?: number; // Durée en millisecondes (par défaut: 5000ms)
  dismissible?: boolean; // Peut être fermée manuellement (par défaut: true)
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  public notifications$: Observable<Notification[]> = this.notificationsSubject.asObservable();
  
  private nextId = 1;

  /**
   * Affiche une notification de succès.
   */
  success(message: string, duration: number = 5000): void {
    this.show({
      id: this.nextId++,
      type: 'success',
      message,
      duration,
      dismissible: true
    });
  }

  /**
   * Affiche une notification d'erreur.
   */
  error(message: string, duration: number = 7000): void {
    this.show({
      id: this.nextId++,
      type: 'error',
      message,
      duration,
      dismissible: true
    });
  }

  /**
   * Affiche une notification d'avertissement.
   */
  warning(message: string, duration: number = 6000): void {
    this.show({
      id: this.nextId++,
      type: 'warning',
      message,
      duration,
      dismissible: true
    });
  }

  /**
   * Affiche une notification d'information.
   */
  info(message: string, duration: number = 5000): void {
    this.show({
      id: this.nextId++,
      type: 'info',
      message,
      duration,
      dismissible: true
    });
  }

  /**
   * Affiche une notification.
   */
  private show(notification: Notification): void {
    const notifications = this.notificationsSubject.value;
    this.notificationsSubject.next([...notifications, notification]);

    // Supprimer automatiquement après la durée spécifiée
    if (notification.duration && notification.duration > 0) {
      setTimeout(() => {
        this.remove(notification.id);
      }, notification.duration);
    }
  }

  /**
   * Supprime une notification.
   */
  remove(id: number): void {
    const notifications = this.notificationsSubject.value.filter(n => n.id !== id);
    this.notificationsSubject.next(notifications);
  }

  /**
   * Supprime toutes les notifications.
   */
  clear(): void {
    this.notificationsSubject.next([]);
  }
}

