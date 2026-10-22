import { TestBed } from '@angular/core/testing';

import { Pagination } from './pagination';

describe('Pagination', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Pagination] }).compileComponents();
  });

  it('shows nothing when there is a single page', async () => {
    const fixture = TestBed.createComponent(Pagination);
    fixture.componentRef.setInput('page', 0);
    fixture.componentRef.setInput('totalPages', 1);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('.ui.pagination')).toBeNull();
  });

  it('shows every page when there are few of them', async () => {
    const fixture = TestBed.createComponent(Pagination);
    fixture.componentRef.setInput('page', 0);
    fixture.componentRef.setInput('totalPages', 5);
    await fixture.whenStable();

    const pages = (fixture.nativeElement as HTMLElement).querySelectorAll('.ui.pagination .item');
    expect(pages.length).toBeGreaterThan(0);
    expect(fixture.componentInstance.visiblePages()).toEqual([0, 1, 2, 3, 4]);
  });

  it('keeps the pager small when there are many pages', async () => {
    const fixture = TestBed.createComponent(Pagination);
    fixture.componentRef.setInput('page', 10);
    fixture.componentRef.setInput('totalPages', 100);
    await fixture.whenStable();

    const visible = fixture.componentInstance.visiblePages();
    expect(visible.length).toBeLessThanOrEqual(7);
    expect(visible).toContain(10);
    expect(visible).toContain(0);
    expect(visible).toContain(99);
  });
});
