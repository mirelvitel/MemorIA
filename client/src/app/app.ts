import {Component, inject, signal} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {HttpClient} from '@angular/common/http';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  standalone: true,
  styleUrl: './app.css'
})
export class App {
  private http = inject(HttpClient);

  status = signal<'idle' | 'checking' | 'ok' | 'error'>('idle');

  checkApi() {
    this.status.set('checking');
    this.http.get('/api/test', { responseType: 'text' })
      .subscribe({
        next: () => this.status.set('ok'),
        error: () => this.status.set('error')
      });
  }
}
