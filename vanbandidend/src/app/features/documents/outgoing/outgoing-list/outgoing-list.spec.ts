import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OutgoingList } from './outgoing-list';

describe('OutgoingList', () => {
  let component: OutgoingList;
  let fixture: ComponentFixture<OutgoingList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OutgoingList]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OutgoingList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
