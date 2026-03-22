import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Subject, interval, startWith, switchMap, takeUntil } from 'rxjs';

interface PageMetric {
  page: string;
  count: number;
}

interface DashboardMetrics {
  activeUsers: number;
  activeSessions: number;
  activeSessionsForSameUser: number;
  referenceUserId: string | null;
  topPages: PageMetric[];
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  template: `
    <main class="container">
      <h1>Ecommerce Analytics Dashboard</h1>
      <p class="subtitle">Auto-refreshes every 30 seconds</p>

      <section class="cards">
        <article class="card">
          <h2>Active Users (last 5m)</h2>
          <p>{{ metrics.activeUsers }}</p>
        </article>

        <article class="card">
          <h2>Active Sessions (last 5m)</h2>
          <p>{{ metrics.activeSessions }}</p>
        </article>

        <article class="card">
          <h2>Sessions For Same User</h2>
          <p>{{ metrics.activeSessionsForSameUser }}</p>
          <small *ngIf="metrics.referenceUserId">User: {{ metrics.referenceUserId }}</small>
        </article>
      </section>

      <section class="table-wrap">
        <h2>Top 5 Pages (last 15m)</h2>
        <table>
          <thead>
            <tr>
              <th>Page URL</th>
              <th>Views</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let page of metrics.topPages">
              <td>{{ page.page }}</td>
              <td>{{ page.count }}</td>
            </tr>
          </tbody>
        </table>
      </section>
    </main>
  `,
  styles: [
    `
      .container {
        max-width: 950px;
        margin: 2rem auto;
        font-family: Arial, sans-serif;
        padding: 0 1rem;
      }
      .subtitle {
        color: #666;
        margin-top: -0.3rem;
        margin-bottom: 1.2rem;
      }
      .cards {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
        gap: 1rem;
        margin-bottom: 1.5rem;
      }
      .card {
        border: 1px solid #ddd;
        border-radius: 8px;
        padding: 1rem;
        background: #fff;
      }
      .card h2 {
        margin: 0;
        font-size: 1rem;
        color: #444;
      }
      .card p {
        margin: 0.5rem 0 0;
        font-size: 1.8rem;
        font-weight: bold;
      }
      .card small {
        color: #666;
      }
      .table-wrap {
        border: 1px solid #ddd;
        border-radius: 8px;
        padding: 1rem;
        background: #fff;
      }
      table {
        width: 100%;
        border-collapse: collapse;
      }
      th,
      td {
        text-align: left;
        padding: 0.6rem 0.4rem;
        border-bottom: 1px solid #eee;
      }
    `
  ]
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly destroy$ = new Subject<void>();
  private readonly apiBase = 'http://localhost:18080/api';

  metrics: DashboardMetrics = {
    activeUsers: 0,
    activeSessions: 0,
    activeSessionsForSameUser: 0,
    referenceUserId: null,
    topPages: []
  };

  ngOnInit(): void {
    interval(30000)
      .pipe(
        startWith(0),
        switchMap(() => this.http.get<DashboardMetrics>(`${this.apiBase}/metrics`)),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (response) => {
          this.metrics = response;
        },
        error: () => {
          this.metrics = {
            activeUsers: 0,
            activeSessions: 0,
            activeSessionsForSameUser: 0,
            referenceUserId: null,
            topPages: []
          };
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
