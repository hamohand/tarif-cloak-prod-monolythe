import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SearchListLotsComponent } from './search-list-lots.component';

describe('SearchListLotsComponent', () => {
  let component: SearchListLotsComponent;
  let fixture: ComponentFixture<SearchListLotsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SearchListLotsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(SearchListLotsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
