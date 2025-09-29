import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OutgoingDetail } from './outgoing-detail';

describe('OutgoingDetail', () => {
  let component: OutgoingDetail;
  let fixture: ComponentFixture<OutgoingDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OutgoingDetail]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OutgoingDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
