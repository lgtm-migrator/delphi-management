import { Injectable } from '@angular/core';
import { ApiService } from './api/api.service';
import { JwtHelperService } from '@auth0/angular-jwt';

const TOKEN_IDENT = 'token';
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private helperService: JwtHelperService;
  constructor(private apiService: ApiService) {
    this.helperService = new JwtHelperService();
  }

  login(username: string, password: string) {
    this.apiService.login(username, password).subscribe((token: string) => {
      console.log('got token', token);
      localStorage.setItem(TOKEN_IDENT, token);
    });
  }

  isValid(): boolean {
    return this.helperService.isTokenExpired(this.getToken());
  }

  getToken(): string {
    return localStorage.getItem(TOKEN_IDENT);
  }
}
