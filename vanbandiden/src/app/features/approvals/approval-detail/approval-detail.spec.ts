import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ApprovalDetail } from './approval-detail';

describe('ApprovalDetail', () => {
  let component: ApprovalDetail;
  let fixture: ComponentFixture<ApprovalDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApprovalDetail]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ApprovalDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
