import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'viDate'
})
export class ViDatePipe implements PipeTransform {

  transform(value: unknown, ...args: unknown[]): unknown {
    return null;
  }

}
