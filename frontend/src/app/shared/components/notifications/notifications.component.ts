import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService, Notification } from '../../../core/services/notification.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="notifications-container">
      @for (notification of notifications; track notification.id) {
        <div class="notification" 
             [class]="'notification-' + notification.type">
          <div class="notification-content">
            <div class="notification-icon">
              @switch (notification.type) {
                @case ('success') { ✅ }
                @case ('error') { ❌ }
                @case ('warning') { ⚠️ }
                @case ('info') { ℹ️ }
              }
            </div>
            <div class="notification-message">{{ notification.message }}</div>
            @if (notification.dismissible !== false) {
              <button class="notification-close" (click)="remove(notification.id)">×</button>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .notifications-container {
      position: fixed;
      top: 80px;
      right: 20px;
      z-index: 10000;
      display: flex;
      flex-direction: column;
      gap: 10px;
      max-width: 400px;
    }

    .notification {
      background: #e8e8e8;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      padding: 1rem;
      animation: slideIn 0.3s ease-out;
      border-left: 4px solid;
    }

    @keyframes slideIn {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }

    .notification-success {
      border-left-color: #4caf50;
      background: #f1f8f4;
    }

    .notification-error {
      border-left-color: #f44336;
      background: #fff5f5;
    }

    .notification-warning {
      border-left-color: #ff9800;
      background: #fffbf0;
    }

    .notification-info {
      border-left-color: #2196f3;
      background: #f0f7ff;
    }

    .notification-content {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
    }

    .notification-icon {
      font-size: 1.5rem;
      flex-shrink: 0;
    }

    .notification-message {
      flex: 1;
      color: #333;
      font-size: 0.95rem;
      line-height: 1.5;
    }

    .notification-close {
      background: none;
      border: none;
      font-size: 1.5rem;
      color: #666;
      cursor: pointer;
      padding: 0;
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      transition: all 0.2s ease;
      flex-shrink: 0;
    }

    .notification-close:hover {
      background: rgba(0, 0, 0, 0.1);
      color: #000;
    }

    /* Animation de sortie */
    .notification.removing {
      animation: slideOut 0.3s ease-in forwards;
    }

    @keyframes slideOut {
      from {
        transform: translateX(0);
        opacity: 1;
      }
      to {
        transform: translateX(100%);
        opacity: 0;
      }
    }
  `]
})
export class NotificationsComponent implements OnInit {
  private notificationService = inject(NotificationService);

  notifications: Notification[] = [];

  ngOnInit() {
    this.notificationService.notifications$.subscribe(notifications => {
      this.notifications = notifications;
    });
  }

  remove(id: number) {
    this.notificationService.remove(id);
  }
}

