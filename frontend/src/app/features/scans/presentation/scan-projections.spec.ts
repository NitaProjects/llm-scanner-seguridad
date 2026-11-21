import { evaluation, execution, finding, id, scanId, score } from '../../../../testing/fixtures';
import { findingCorrelated, riskBands, selectedFinding, testRails } from './scan-projections';

describe('Risk Intelligence pure projections', () => {
  function complete() {
    const item = finding(); const test = execution();
    const result = evaluation(0, { finalVerdict: 'VULNERABLE', findingId: item.id });
    return { item, test, result };
  }
  it('retains original records and verifies the full public chain without deriving outcomes', () => {
    const { item, test, result } = complete(); const rails = testRails(scanId, [test], [result], [item]);
    expect(rails).toHaveLength(4); expect(rails[0].execution).toBe(test); expect(rails[0].evaluation).toBe(result);
    expect(rails[0].findings).toEqual([item]); expect(findingCorrelated(item, rails)).toBe(true);
    expect(rails[1].execution).toBeNull(); expect(rails[1].evaluation).toBeNull();
  });
  it.each(['scanId', 'securityTestCode', 'securityTestRevision', 'category', 'testExecutionId'] as const)('does not join an evaluation with incompatible %s', field => {
    const { item, test, result } = complete();
    const broken = { ...result, [field]: field === 'securityTestRevision' ? 2 : field === 'category' ? 'LLM02' : id(99) } as typeof result;
    const rails = testRails(scanId, [test], [broken], [item]);
    expect(rails[0].evaluation).toBeNull(); expect(findingCorrelated(item, rails)).toBe(false);
  });
  it.each(['scanId', 'securityTestCode', 'securityTestRevision', 'category', 'testExecutionId', 'evaluationResultId'] as const)('does not attribute a Finding with incompatible %s', field => {
    const { item, test, result } = complete();
    const broken = { ...item, [field]: field === 'securityTestRevision' ? 2 : field === 'category' ? 'LLM02' : id(99) } as typeof item;
    expect(findingCorrelated(broken, testRails(scanId, [test], [result], [broken]))).toBe(false);
  });
  it('rejects duplicate IDs, ambiguous executions and mismatched findingId references', () => {
    const { item, test, result } = complete();
    expect(testRails(scanId, [test, { ...test }], [result], [item])[0].execution).toBeNull();
    expect(testRails(scanId, [test], [result, { ...result }], [item])[0].evaluation).toBeNull();
    expect(testRails(scanId, [test], [result], [item, { ...item }])[0].findings).toEqual([]);
    expect(testRails(scanId, [test], [{ ...result, findingId: id(98) }], [item])[0].findings).toEqual([]);
    expect(selectedFinding([item, { ...item }], item.id)).toBeNull();
    expect(selectedFinding([item], item.id.toUpperCase())).toBe(item);
  });
  it('keeps score zero independent of a correlated inconclusive outcome', () => {
    const result = evaluation(0, { finalVerdict: 'INCONCLUSIVE', confidence: 0, outcomeReason: 'CONTEXT_UNAVAILABLE' });
    const bands = riskBands(score().categories, testRails(scanId, [execution()], [result], []));
    expect(bands[0].score?.score).toBe(0); expect(bands[0].rail?.evaluation?.finalVerdict).toBe('INCONCLUSIVE');
  });
  it('keeps all category identities and received evaluation when no score was received', () => {
    const bands = riskBands([], testRails(scanId, [execution()], [evaluation()], []));
    expect(bands.map(row => row.category)).toEqual(['LLM01', 'LLM02', 'LLM08', 'LLM10']);
    expect(bands.every(row => row.score === null)).toBe(true); expect(bands[0].rail?.evaluation).not.toBeNull();
  });
  it('does not choose an arbitrary category score or synthesize an evaluation from counts', () => {
    const categories = score().categories;
    const bands = riskBands([categories[0], categories[0]], []);
    expect(bands[0].score).toBeNull(); expect(bands[0].rail).toBeNull();
    expect(riskBands(categories, [])[0].rail).toBeNull();
  });
  it('preserves orphan Findings for inspection while declining a verified chain', () => {
    const { item } = complete(); expect(selectedFinding([item], item.id)).toBe(item);
    expect(findingCorrelated(item, testRails(scanId, [], [], [item]))).toBe(false);
  });
});
