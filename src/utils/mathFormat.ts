const REPLACEMENTS: Array<[string, string]> = [
  ['\\tfrac12', '½'],
  ['\\tfrac{1}{2}', '½'],
  ['\\dfrac{u^2}{2g}', '(u² / 2g)'],
  ['\\dfrac{u^2\\sin2\\theta}{g}', '(u² sin 2θ) / g'],
  ['\\dfrac{0.693}{k}', '(0.693 / k)'],
  ['\\dfrac{0.0591}{n}', '(0.0591 / n)'],
  ['\\dfrac{-b\\pm\\sqrt{b^2-4ac}}{2a}', '(-b ± √(b² - 4ac)) / 2a'],
  ['\\dfrac{d\\Phi_B}{dt}', 'dΦ_B/dt'],
  ['\\dfrac{1}{R}', '1/R'],
  ['\\dfrac1R', '1/R'],
  ['\\dfrac14', '1/4'],
  ['\\dfrac12', '1/2'],
  ['\\dfrac1{R_{eq}}', '1/R_eq'],
  ['\\dfrac{1}{R_1}', '1/R₁'],
  ['\\dfrac{1}{R_2}', '1/R₂'],
  ['\\text{m/s}^2', 'm/s²'],
  ['\\text{m/s}', 'm/s'],
  ['\\text{s}^{-1}', 's⁻¹'],
  ['\\text{kg}', 'kg'],
  ['\\text{Fe}^{3+}', 'Fe³⁺'],
  ['\\text{Ar}', 'Ar'],
  ['\\text{N}', 'N'],
  ['\\text{J}', 'J'],
  ['\\text{m}', 'm'],
  ['\\text{s}', 's'],
  ['\\varepsilon_0', 'ε₀'],
  ['\\varepsilon', 'ε'],
  ['\\Phi_B', 'Φ_B'],
  ['\\Phi', 'Φ'],
  ['\\Omega', 'Ω'],
  ['\\theta', 'θ'],
  ['\\Delta', 'Δ'],
  ['\\Rightarrow', '⇒'],
  ['\\cdot', '·'],
  ['\\cdots', '…'],
  ['\\times', '×'],
  ['\\pm', '±'],
  ['\\sin', 'sin'],
  ['\\cos', 'cos'],
  ['\\log', 'log'],
  ['\\sqrt', '√'],
  ['\\circ', '°'],
  ['\\;', ' '],
  ['\\,', ' '],
  ['^\\circ', '°'],
  ['^{2}', '²'],
  ['^2', '²'],
  ['^{3}', '³'],
  ['^3', '³'],
  ['^{23}', '²³'],
  ['^{20}', '²⁰'],
  ['^{3+}', '³⁺'],
  ['\\text{', ''],
  ['$$', ''],
  ['$', ''],
];

export function formatMathText(raw: string): string {
  if (!raw.includes('$') && !raw.includes('\\')) return raw;
  let s = raw;
  for (const [k, v] of REPLACEMENTS) {
    s = s.split(k).join(v);
  }
  s = s.replace(/\\dfrac\{([^}]*)\}\{([^}]*)\}/g, '($1)/($2)');
  s = s.replace(/\\frac\{([^}]*)\}\{([^}]*)\}/g, '($1)/($2)');
  s = s.replaceAll('{', '').replaceAll('}', '').replaceAll('\\', '');
  return s.trim();
}
