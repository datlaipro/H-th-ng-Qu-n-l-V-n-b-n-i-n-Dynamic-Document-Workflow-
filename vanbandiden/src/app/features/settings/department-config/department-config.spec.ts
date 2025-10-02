import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DepartmentConfig } from './department-config';

describe('DepartmentConfig', () => {
  let component: DepartmentConfig;
  let fixture: ComponentFixture<DepartmentConfig>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DepartmentConfig]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DepartmentConfig);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
