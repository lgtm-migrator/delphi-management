/*
 * Copyright (C) 2018 The Delphi Team.
 * See the LICENCE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Inject, Injectable, Optional} from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Configuration} from '../configuration';
import {BASE_PATH, INSTANCES, NUMBER_OF_INSTANCES, SYS_INFO} from '../variables';
import {CustomHttpUrlEncodingCodec} from '../encoder';
import {Instance} from '..';
import {SysInfo} from '../model/sysInfo';


@Injectable({
  providedIn: 'root'
})
export class ApiService {

  protected basePath = '';
  public defaultHeaders = new HttpHeaders();
  public configuration = new Configuration();

  constructor(protected httpClient: HttpClient,
              @Optional()@Inject(BASE_PATH) basePath: string,
              @Optional() configuration: Configuration) {
    if (basePath) {
      this.basePath = basePath;
    }
    if (configuration) {
      this.configuration = configuration;
      this.basePath = basePath || configuration.basePath || this.basePath;
    }
  }

  public getSysInfo(reportProgress: boolean = false): Observable<SysInfo> {
    let headers = this.defaultHeaders;

    // to determine the Accept header
    const httpHeaderAccepts: string[] = [
      'application/json'
    ];
    const httpHeaderAcceptSelected: string | undefined = this.configuration.selectHeaderAccept(httpHeaderAccepts);
    if (httpHeaderAcceptSelected !== undefined) {
      headers = headers.set('Accept', httpHeaderAcceptSelected);
    }
    return this.httpClient.get<SysInfo>(`${this.basePath}${SYS_INFO}`,
      {headers: headers, observe: 'body', reportProgress: reportProgress});
    }

  /**
   * Find number of running instances
   * How many instances per type are running
   * @param componentType
   * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
   * @param reportProgress flag to report request and response progress.
   */
  public getNumberOfInstances(componentType: string, observe: any = 'body', reportProgress: boolean = false ): Observable<number> {
    return this.get(NUMBER_OF_INSTANCES, componentType);
  }

  /**
   * Find number of running instances
   * How many instances per type are running
   * @param componentType
   * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
   * @param reportProgress flag to report request and response progress.
   */
  public getInstances(componentType: string, observe: any = 'body', reportProgress: boolean = false ): Observable<Array<Instance>> {
    return this.get(INSTANCES, componentType);
  }

  private get(endpoint: string, componentType: string, observe: any = 'body', reportProgress: boolean = false ): any {
    if (componentType === null || componentType === undefined) {
      throw new Error('Required parameter componentType was null or undefined when calling getInstanceNumber.');
    }

    let queryParameters = new HttpParams({encoder: new CustomHttpUrlEncodingCodec()});
    if (componentType !== undefined) {
      queryParameters = queryParameters.set('componentType', <any>componentType);
    }

    let headers = this.defaultHeaders;

    // to determine the Accept header
    const httpHeaderAccepts: string[] = [
      'application/json'
    ];
    const httpHeaderAcceptSelected: string | undefined = this.configuration.selectHeaderAccept(httpHeaderAccepts);
    if (httpHeaderAcceptSelected !== undefined) {
      headers = headers.set('Accept', httpHeaderAcceptSelected);
    }

    return this.httpClient.get<Instance | number>(`${this.basePath}${endpoint}`,
      {
        params: queryParameters,
        withCredentials: this.configuration.withCredentials,
        headers: headers,
        observe: observe,
        reportProgress: reportProgress
      }
    );
  }
}