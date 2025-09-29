import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OutgoingCompose } from './outgoing-compose';

describe('OutgoingCompose', () => {
  let component: OutgoingCompose;
  let fixture: ComponentFixture<OutgoingCompose>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OutgoingCompose]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OutgoingCompose);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
